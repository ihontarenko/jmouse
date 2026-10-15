package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.jpa.credential.CredentialContexts;
import org.jmouse.ai.jpa.credential.CredentialProtection;
import org.jmouse.ai.jpa.credential.ProviderCredentials;
import org.jmouse.ai.jpa.entity.AiProviderSettings;
import org.jmouse.ai.provider.ProviderException;
import org.jmouse.ai.provider.ProviderSettings;
import org.jmouse.ai.provider.ProviderSettingsSource;

import java.util.List;

/**
 * The settings in force, read from the database on every call.
 *
 * <p>⚠️ <strong>Every call, not startup.</strong> Somebody rotating a leaked key expects the next
 * request to use the new one; settings bound into a bean at startup mean a restart instead, and a
 * restart is exactly what nobody wants to be doing at that moment. One indexed query against one row,
 * next to an HTTP call to a language model that will take a thousand times longer.
 *
 * <p>⚠️ <strong>The application name is fixed at construction.</strong> It is the key these settings are
 * <em>looked up</em> by and has no business travelling in a request — putting it there is precisely
 * what stopped the gateway this was learned from being usable as a library.
 *
 * <p>A product that wants caching wraps this or replaces it; a caller cannot tell, which is why that
 * decision belongs behind the interface rather than in front of it.
 *
 * <p>⚠️ <strong>The stored key is opened here, and only here.</strong> The column may hold a sealed
 * value or a legacy one; {@link CredentialProtection} reads both, and refuses a sealed one when no
 * credential key is configured rather than handing ciphertext to a provider.
 */
public final class JpaProviderSettingsSource implements ProviderSettingsSource {

    private final EntityManagerFactory entityManagerFactory;
    private final String               application;
    private final CredentialProtection credentialProtection;
    private final ProviderCredentials  providerCredentials;

    public JpaProviderSettingsSource(
            EntityManagerFactory entityManagerFactory,
            String               application,
            CredentialProtection credentialProtection) {
        this(entityManagerFactory, application, credentialProtection, null);
    }

    /** @param providerCredentials resolves a row that names a vault credential, at each call; may be null */
    public JpaProviderSettingsSource(
            EntityManagerFactory entityManagerFactory,
            String               application,
            CredentialProtection credentialProtection,
            ProviderCredentials  providerCredentials) {

        this.entityManagerFactory = entityManagerFactory;
        this.application          = application;
        this.credentialProtection = credentialProtection;
        this.providerCredentials  = providerCredentials;
    }

    @Override
    public ProviderSettings settings() {
        return settings(GENERAL, org.jmouse.ai.model.AiCapability.CHAT);
    }

    /**
     * The provider configured for one purpose, or the general one.
     *
     * <h2>⚠️ TWO QUERIES, AND THE SECOND ONE IS THE MIGRATION</h2>
     *
     * <p>The specific purpose is looked for first. Finding nothing is <strong>not</strong> a failure:
     * it falls back to the row with no purpose, which is what every installation configured before
     * purposes existed has. So nothing has to be reconfigured, and a product asking for a purpose
     * nobody has set up gets the general provider instead of a refusal.
     *
     * <p>⚠️ Asked in that order rather than in one query with an {@code order by}. A single query
     * sorting purpose-first works and is unreadable, and the day somebody adds a third fallback it
     * silently sorts wrong. Two named lookups say which one won.
     */
    @Override
    public ProviderSettings settings(String purpose) {
        return settings(purpose, org.jmouse.ai.model.AiCapability.CHAT);
    }

    @Override
    public ProviderSettings settings(String purpose, org.jmouse.ai.model.AiCapability capability) {
        String wanted = purpose == null || purpose.isBlank() ? GENERAL : purpose.trim();

        List<AiProviderSettings> forPurpose = activeFor(wanted);

        if (!forPurpose.isEmpty()) {
            return asSettings(onlyCapable(forPurpose, capability));
        }

        // ⚠️ GENERAL is asked for as null, because that is what an un-purposed row carries. Asking
        // for the literal string would miss every row that predates this column.
        List<AiProviderSettings> general = GENERAL.equals(wanted) ? forPurpose : activeFor(GENERAL);
        List<AiProviderSettings> unpurposed = general.isEmpty() ? activeFor(null) : general;

        if (unpurposed.isEmpty()) {
            throw new ProviderException(
                    "No AI provider is active for '" + application + "'"
                    + (GENERAL.equals(wanted) ? "" : " for '" + wanted + "' or in general")
                    + ", so there is nothing to call. Configure one, or mark an existing row active.");
        }

        return asSettings(onlyCapable(unpurposed, capability));
    }

    private AiProviderSettings onlyCapable(List<AiProviderSettings> rows, org.jmouse.ai.model.AiCapability capability) {
        var capable = rows.stream().filter(row -> row.getCapabilities().contains(capability)).toList();
        if (capable.isEmpty()) {
            throw new ProviderException("No active configuration declares the required model capability.");
        }
        return only(capable, "for the required capability");
    }

    @Override
    public ProviderSettings settingsById(String identifier, org.jmouse.ai.model.AiCapability capability) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            var rows = entityManager.createQuery("""
                    SELECT configured FROM AiProviderSettings configured
                    WHERE configured.application = :application AND configured.id = :identifier AND configured.active = true
                    """, AiProviderSettings.class).setParameter("application", application)
                    .setParameter("identifier", identifier).getResultList();
            return asSettings(onlyCapable(rows, capability));
        });
    }

    /**
     * ⚠️ {@code null} means the un-purposed row, and JPQL cannot express that with a parameter —
     * {@code = :purpose} is never true against null. Hence the branch rather than one query.
     */
    private List<AiProviderSettings> activeFor(String purpose) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            String where = purpose == null
                           ? "configured.purpose is null"
                           : "configured.purpose = :purpose";

            var query = entityManager.createQuery("""
                            select configured
                              from AiProviderSettings configured
                             where configured.application = :application
                               and configured.active      = true
                               and %s
                             order by configured.updatedAt desc
                            """.formatted(where), AiProviderSettings.class)
                    .setParameter("application", application);

            if (purpose != null) {
                query.setParameter("purpose", purpose);
            }
            return query.getResultList();
        });
    }

    /**
     * ⚠️ Refuses rather than picking, and says how many. Two active rows for one purpose is a
     * configuration mistake whose symptom would otherwise be a provider that changes between requests
     * for no visible reason — a far worse afternoon than a refusal that names the problem.
     */
    private AiProviderSettings only(List<AiProviderSettings> active, String describedAs) {
        if (active.size() > 1) {
            throw new ProviderException(
                    "There are " + active.size() + " active AI provider rows " + describedAs + " for '"
                    + application + "' and only one may be. Deactivate all but the one that should be "
                    + "in force.");
        }
        return active.getFirst();
    }

    private ProviderSettings asSettings(AiProviderSettings row) {
        if (row.getCredentialName() != null) {
            if (providerCredentials == null) {
                throw new ProviderException("The AI provider configuration '" + row.getId() + "' takes its key from the vault "
                        + "credential '" + row.getCredentialName() + "', and this application has no vault configured.");
            }

            ProviderCredentials.Resolved resolved;

            try {
                resolved = providerCredentials.resolve(row.getCredentialName());
            } catch (RuntimeException unresolved) {
                throw new ProviderException("The vault credential '" + row.getCredentialName() + "' of the AI provider "
                        + "configuration '" + row.getId() + "' cannot be read: " + unresolved.getMessage());
            }

            return new ProviderSettings(row.getProvider(), row.getModel(), resolved.apiKey(), resolved.apiUrl(),
                    row.getMaximumTokens(), row.getCapabilities(), row.getId(), row.getRevision());
        }

        return new ProviderSettings(
                row.getProvider(), row.getModel(), credentialProtection.reveal(row.getApiKey(), CredentialContexts.providerConfiguration(row.getId())), row.getApiUrl(),
                row.getMaximumTokens(),
                row.getCapabilities(), row.getId(), row.getRevision());
    }
}
