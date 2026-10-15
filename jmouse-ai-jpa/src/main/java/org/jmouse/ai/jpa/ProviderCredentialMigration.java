package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.jpa.credential.CredentialContexts;
import org.jmouse.ai.jpa.credential.CredentialProtection;
import org.jmouse.ai.jpa.credential.ProviderCredentials;
import org.jmouse.ai.jpa.entity.AiProviderSettings;

import java.util.List;
import java.util.Objects;

/**
 * 🚚 Moves every provider configuration still holding its URL and key on the row into the vault — its own
 * credential {@code ai/providers/<id>} — and empties those columns.
 *
 * <p>Run at start-up by an installation that has a vault, so adopting one is a restart rather than a data
 * migration somebody has to write. Idempotent: a row that already names a credential is not touched.
 */
public final class ProviderCredentialMigration {

    private final EntityManagerFactory entityManagerFactory;
    private final String               application;
    private final CredentialProtection credentialProtection;
    private final ProviderCredentials  providerCredentials;

    public ProviderCredentialMigration(EntityManagerFactory entityManagerFactory, String application,
                                       CredentialProtection credentialProtection, ProviderCredentials providerCredentials) {
        this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "entity manager factory");
        this.application          = Objects.requireNonNull(application, "application");
        this.credentialProtection = Objects.requireNonNull(credentialProtection, "credential protection");
        this.providerCredentials  = Objects.requireNonNull(providerCredentials, "provider credentials");
    }

    /** @return how many configurations were moved */
    public int migrate() {
        List<AiProviderSettings> rows = OwnTransaction.call(entityManagerFactory, entityManager -> entityManager.createQuery("""
                        select configured
                          from AiProviderSettings configured
                         where configured.application    = :application
                           and configured.credentialName is null
                        """, AiProviderSettings.class)
                .setParameter("application", application)
                .getResultList());

        for (AiProviderSettings row : rows) {
            String apiKey = credentialProtection.reveal(row.getApiKey(), CredentialContexts.providerConfiguration(row.getId()));
            String name   = providerCredentials.saveOwn(row.getId(), row.getApiUrl(), apiKey);

            OwnTransaction.run(entityManagerFactory, entityManager -> entityManager
                    .find(AiProviderSettings.class, row.getId())
                    .useCredential(name));
        }

        return rows.size();
    }
}
