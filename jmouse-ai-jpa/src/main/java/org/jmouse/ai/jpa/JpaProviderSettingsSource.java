package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManagerFactory;
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
 */
public final class JpaProviderSettingsSource implements ProviderSettingsSource {

    private final EntityManagerFactory entityManagerFactory;
    private final String               application;

    public JpaProviderSettingsSource(EntityManagerFactory entityManagerFactory, String application) {
        this.entityManagerFactory = entityManagerFactory;
        this.application          = application;
    }

    @Override
    public ProviderSettings settings() {
        List<AiProviderSettings> active = OwnTransaction.call(entityManagerFactory,
                entityManager -> entityManager.createQuery("""
                                select configured
                                  from AiProviderSettings configured
                                 where configured.application = :application
                                   and configured.active      = true
                                 order by configured.updatedAt desc
                                """, AiProviderSettings.class)
                        .setParameter("application", application)
                        .getResultList());

        if (active.isEmpty()) {
            throw new ProviderException(
                    "No AI provider is active for '" + application + "', so there is nothing to call. "
                    + "Configure one, or mark an existing row active.");
        }

        // ⚠️ Refuses rather than picking, and names both. Two active rows is a configuration mistake
        // whose symptom would otherwise be a provider that changes between requests for no visible
        // reason — which is a far worse afternoon than a startup-shaped refusal.
        if (active.size() > 1) {
            throw new ProviderException(
                    "There are " + active.size() + " active AI provider rows for '" + application
                    + "' and only one may be. Deactivate all but the one that should be in force.");
        }

        return asSettings(active.getFirst());
    }

    private ProviderSettings asSettings(AiProviderSettings row) {
        return new ProviderSettings(
                row.getProvider(), row.getModel(), row.getApiKey(), row.getApiUrl(), row.getMaximumTokens());
    }
}
