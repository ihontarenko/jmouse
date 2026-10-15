package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.administration.ProviderAdministration;
import org.jmouse.ai.jpa.credential.CredentialContexts;
import org.jmouse.ai.jpa.credential.CredentialFingerprint;
import org.jmouse.ai.jpa.credential.CredentialProtection;
import org.jmouse.ai.jpa.credential.ProviderCredentials;
import org.jmouse.ai.jpa.entity.AiProviderSettings;
import org.jmouse.ai.provider.ProviderCatalog;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The provider configuration, administered as the rows {@link JpaProviderSettingsSource} reads.
 *
 * <p><strong>Here rather than in each product, because the rules are this module's own.</strong>
 * "Exactly one row is in force" is not a preference somebody adopted — it is the condition the settings
 * source refuses to resolve without, loudly, at the moment somebody asks a question. Leaving the rule to
 * whoever wrote the screen meant every adopter re-derived it, and the one that got it wrong found out
 * through an application that had stopped answering.
 *
 * <p>⚠️ <strong>Every query is filtered by application.</strong> One installation's table can serve
 * several, so a screen must never be able to activate, change or delete somebody else's row. The
 * application name is fixed at construction and has no business travelling in a request — putting it
 * there is precisely what stopped the gateway this design was learned from being usable as a library.
 *
 * <p>⚠️ <strong>The key is write-only.</strong> {@link Configuration} has no field for one, and a draft
 * carrying a blank key keeps whatever is stored — see {@link Draft}.
 *
 * <p>⚠️ <strong>And it is written through {@link CredentialProtection}.</strong> Every key written here
 * is sealed when the installation has a credential key, and carries its {@link CredentialFingerprint}
 * either way. A change that does not replace the key still re-seals a legacy plain value and refreshes
 * the fingerprint, because the provider is part of what the fingerprint identifies.
 */
public final class JpaProviderAdministration implements ProviderAdministration {

    private final EntityManagerFactory entityManagerFactory;
    private final String               application;
    private final CredentialProtection credentialProtection;
    private final ProviderCredentials  providerCredentials;

    public JpaProviderAdministration(
            EntityManagerFactory entityManagerFactory,
            String               application,
            CredentialProtection credentialProtection) {
        this(entityManagerFactory, application, credentialProtection, null);
    }

    /**
     * @param providerCredentials the vault the URL and key are kept in; null keeps them on the row, for an
     *                            installation without a vault
     */
    public JpaProviderAdministration(
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
    public List<String> supportedProviders() {
        return ProviderCatalog.shipped();
    }

    /** The same list with what a screen needs, in the catalogue's own order rather than alphabetically. */
    @Override
    public List<SupportedProvider> describeSupportedProviders() {
        return ProviderCatalog.describeShipped().stream()
                .map(shipped -> new SupportedProvider(
                        shipped.name(), shipped.defaultApiUrl(), shipped.requiresKey(), shipped.note(), shipped.capabilities()))
                .toList();
    }

    @Override
    public List<Configuration> configurations() {
        return OwnTransaction.call(entityManagerFactory, entityManager -> entityManager.createQuery("""
                                select configured
                                  from AiProviderSettings configured
                                 where configured.application = :application
                                 order by configured.createdAt asc
                                """, AiProviderSettings.class)
                        .setParameter("application", application)
                        .getResultList())
                .stream()
                .map(this::describe)
                .toList();
    }

    @Override
    public Optional<Configuration> find(String id) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> row(entityManager, id)).map(this::describe);
    }

    @Override
    public Configuration add(Draft draft) {
        String        provider = requireSupported(draft.provider());
        requireCapabilities(provider, draft);
        LocalDateTime now      = LocalDateTime.now();
        String        id       = UUID.randomUUID().toString();

        if (providerCredentials != null) {
            String credentialName = credentialFor(id, draft);

            return describe(OwnTransaction.call(entityManagerFactory, entityManager -> {
                AiProviderSettings created = new AiProviderSettings(id, application, provider, null, null, null,
                        draft.model(), draft.maximumTokens(), false, now, now);

                created.setPurpose(draft.purpose());
                created.setCapabilities(draft.capabilities());
                created.useCredential(credentialName);

                entityManager.persist(created);

                return created;
            }));
        }

        String apiKey = blankToNull(draft.apiKey());

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiProviderSettings created = new AiProviderSettings(
                    id,
                    application,
                    provider,
                    credentialProtection.protect(apiKey, CredentialContexts.providerConfiguration(id)),
                    CredentialFingerprint.of(provider, apiKey),
                    blankToNull(draft.apiUrl()),
                    draft.model(),
                    draft.maximumTokens(),
                    // Idle. Typing a key is not saying "start spending money through this one".
                    false,
                    now,
                    now);

            created.setPurpose(draft.purpose());
            created.setCapabilities(draft.capabilities());

            entityManager.persist(created);

            return describe(created);
        });
    }

    @Override
    public Configuration change(String id, Draft draft) {
        String provider = requireSupported(draft.provider());
        requireCapabilities(provider, draft);

        if (providerCredentials != null) {
            AiProviderSettings before = OwnTransaction.call(entityManagerFactory, entityManager -> {
                AiProviderSettings configured = require(entityManager, id);

                requireChangeable(configured, draft);

                return configured;
            });

            String             credentialName = credentialFor(id, draft);
            AiProviderSettings changed        = OwnTransaction.call(entityManagerFactory, entityManager -> {
                AiProviderSettings configured = require(entityManager, id);

                configured.setProvider(provider);
                configured.setModel(draft.model());
                configured.setMaximumTokens(draft.maximumTokens());
                configured.setPurpose(draft.purpose());
                configured.setCapabilities(draft.capabilities());
                configured.setUpdatedAt(LocalDateTime.now());
                configured.useCredential(credentialName);

                entityManager.flush();

                return configured;
            });

            if (ProviderCredentials.isOwn(before.getCredentialName(), id) && !ProviderCredentials.isOwn(credentialName, id)) {
                providerCredentials.removeOwn(id);
            }

            return describe(changed);
        }

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiProviderSettings configured = require(entityManager, id);

            requireChangeable(configured, draft);

            configured.setProvider(provider);
            configured.setModel(draft.model());
            configured.setApiUrl(blankToNull(draft.apiUrl()));
            configured.setMaximumTokens(draft.maximumTokens());
            configured.setPurpose(draft.purpose());
            configured.setCapabilities(draft.capabilities());
            configured.setUpdatedAt(LocalDateTime.now());

            // Blank means "leave it", never "clear it" — see Draft.
            if (draft.carriesKey()) {
                configured.replaceApiKey(
                        credentialProtection.protect(draft.apiKey(), CredentialContexts.providerConfiguration(id)),
                        CredentialFingerprint.of(provider, draft.apiKey()));
            } else if (hasKey(configured)) {
                keepApiKey(configured, provider);
            }

            entityManager.flush();

            return describe(configured);
        });
    }

    @Override
    public Configuration putInForce(String id) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            // Serialize activation across this application's existing rows in a stable lock order.
            entityManager.createQuery("SELECT configured FROM AiProviderSettings configured WHERE configured.application = :application ORDER BY configured.id",
                    AiProviderSettings.class).setParameter("application", application)
                    .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultList();
            AiProviderSettings target = require(entityManager, id);

            // ⚠️ Asked of the provider rather than demanded of every configuration. A model running on
            // this machine has no credential to give, and refusing it for not having one would make the
            // only free-in-every-sense option the one option that cannot be switched on.
            if (ProviderCatalog.requiresKey(target.getProvider()) && !keyConfigured(target)) {
                throw new RefusedException(
                        "This configuration has no key, so every call through it would be refused before "
                        + "it was sent. Add the key first, then put it in force.");
            }

            /*
              ⚠️ Not a tidy-up afterwards — it is the other half of this operation. Two active rows
              for one purpose is the one state the settings source cannot resolve.

              ⚠️ AND IT IS SCOPED TO THE PURPOSE, which is the whole reason purposes are usable at
              all. Deactivating every active row of the application — which is what this did, and
              was right when there was only one — would mean switching on a provider for matching
              switched OFF the assistant. Two features silently fighting over one row, with the
              symptom appearing in whichever one somebody used second.
             */
            entityManager.createQuery("""
                            select configured
                              from AiProviderSettings configured
                             where configured.application = :application
                               and configured.active      = true
                            """, AiProviderSettings.class)
                    .setParameter("application", application)
                    .getResultList()
                    .stream()
                    .filter(inForce -> !inForce.getId().equals(id))
                    .filter(inForce -> samePurpose(inForce.getPurpose(), target.getPurpose()))
                    .filter(inForce -> inForce.getCapabilities().stream().anyMatch(target.getCapabilities()::contains))
                    .forEach(JpaProviderAdministration::deactivate);

            target.setActive(true);
            target.setUpdatedAt(LocalDateTime.now());

            entityManager.flush();

            return describe(target);
        });
    }

    @Override
    public Configuration takeOutOfForce(String id) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiProviderSettings configured = require(entityManager, id);

            deactivate(configured);

            entityManager.flush();

            return describe(configured);
        });
    }

    @Override
    public void discard(String id) {
        String credentialName = OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiProviderSettings configured = require(entityManager, id);

            if (configured.isActive()) {
                throw new RefusedException(
                        "This is the configuration currently in force. Take it out of force, or put "
                        + "another one in force, before deleting it.");
            }

            entityManager.remove(configured);

            return configured.getCredentialName();
        });

        // Its own credential goes with it; a shared one stays, other configurations may name it.
        if (providerCredentials != null && ProviderCredentials.isOwn(credentialName, id)) {
            providerCredentials.removeOwn(id);
        }
    }

    private String requireSupported(String provider) {
        String named = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);

        if (!ProviderCatalog.ships(named)) {
            throw new RefusedException(
                    "'" + provider + "' is not a provider this application can speak to. Choose one of: "
                    + String.join(", ", supportedProviders()) + ".");
        }

        return named;
    }

    private void requireCapabilities(String provider, Draft draft) {
        if (!ProviderCatalog.capabilities(provider).containsAll(draft.capabilities())) {
            throw new RefusedException("The provider does not support the configured model capabilities.");
        }
    }

    /**
     * ⚠️ Always by identifier <em>and</em> application. A bare {@code find} over a table one installation
     * shares between applications is an endpoint that edits somebody else's row.
     */
    private AiProviderSettings require(EntityManager entityManager, String id) {
        return row(entityManager, id).orElseThrow(() -> new RefusedException(
                "No AI provider configuration of this application has the identifier '" + id + "'."));
    }

    private Optional<AiProviderSettings> row(EntityManager entityManager, String id) {
        return entityManager.createQuery("""
                        select configured
                          from AiProviderSettings configured
                         where configured.id          = :id
                           and configured.application = :application
                        """, AiProviderSettings.class)
                .setParameter("id", id)
                .setParameter("application", application)
                .getResultList()
                .stream()
                .findFirst();
    }

    /**
     * The stored key stays — re-sealed if it was still plain or in an older format — with a fingerprint
     * for the provider it now belongs to. A value already in the current format is not re-sealed: nothing
     * would change but the nonce.
     */
    private void keepApiKey(AiProviderSettings configured, String provider) {
        String stored  = configured.getApiKey();
        String context =CredentialContexts.providerConfiguration(configured.getId());
        String apiKey  = credentialProtection.reveal(stored, context);

        if (credentialProtection.needsSealing(stored)) {
            stored = credentialProtection.protect(apiKey, context);
        }

        configured.replaceApiKey(stored, CredentialFingerprint.of(provider, apiKey));
    }

    private static void deactivate(AiProviderSettings configured) {
        configured.setActive(false);
        configured.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Whether a credential is stored — the only question about it anything outside this class may ask.
     */
    private static boolean hasKey(AiProviderSettings configured) {
        return configured.getApiKey() != null && !configured.getApiKey().isBlank();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * ⚠️ The stored times are zoneless columns written from {@link LocalDateTime#now()}, so the zone they
     * were written in is the one running this — the same reading {@link JpaCallCounter} takes.
     */
    /**
     * Whether two rows are competing for the same job.
     *
     * <p>⚠️ Both null is the same purpose — the general one — and that case matters most, because it
     * is every installation configured before purposes existed. {@code Objects.equals} would say so
     * too; this is named so the call site reads as a question about purposes rather than about nulls.
     */
    private static boolean samePurpose(String one, String other) {
        return normalizedPurpose(one).equals(normalizedPurpose(other));
    }

    private static String normalizedPurpose(String value) {
        return value == null || value.isBlank() ? org.jmouse.ai.provider.ProviderSettingsSource.GENERAL : value.trim();
    }

    private static void requireChangeable(AiProviderSettings configured, Draft draft) {
        if (configured.isActive() && (!samePurpose(configured.getPurpose(), draft.purpose())
                || !configured.getCapabilities().equals(draft.capabilities()))) {
            throw new RefusedException("Take the configuration out of force before changing purpose or capabilities.");
        }
    }

    /**
     * The credential a configuration points at after this draft: the shared one it names, else its own,
     * written now with the draft's URL and — when one was typed — its key.
     */
    private String credentialFor(String id, Draft draft) {
        if (draft.credentialName() != null) {
            try {
                providerCredentials.requireExists(draft.credentialName());
            } catch (IllegalArgumentException refusal) {
                throw new RefusedException(refusal.getMessage());
            }

            return draft.credentialName();
        }

        return providerCredentials.saveOwn(id, draft.apiUrl(), draft.apiKey());
    }

    /** Whether a key is there to call with — on the row, or in the vault credential it points at. */
    private boolean keyConfigured(AiProviderSettings configured) {
        if (configured.getCredentialName() == null || providerCredentials == null) {
            return hasKey(configured);
        }

        return providerCredentials.tryResolve(configured.getCredentialName())
                .map(resolved -> resolved.apiKey() != null)
                .orElse(false);
    }

    private Configuration describe(AiProviderSettings configured) {
        String apiUrl = configured.getApiUrl();

        if (configured.getCredentialName() != null && providerCredentials != null) {
            apiUrl = providerCredentials.tryResolve(configured.getCredentialName())
                    .map(ProviderCredentials.Resolved::apiUrl)
                    .orElse(null);
        }

        return new Configuration(
                configured.getId(),
                configured.getProvider(),
                configured.getModel(),
                apiUrl,
                configured.getMaximumTokens(),
                configured.isActive(),
                keyConfigured(configured),
                configured.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                configured.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                configured.getPurpose(),
                configured.getCapabilities(),
                configured.getRevision(),
                configured.getCredentialName());
    }
}
