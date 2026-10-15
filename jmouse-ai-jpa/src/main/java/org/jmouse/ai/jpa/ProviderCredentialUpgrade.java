package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.vault.seal.SecretCipher;
import org.jmouse.ai.jpa.credential.CredentialContexts;
import org.jmouse.ai.jpa.credential.CredentialFingerprint;
import org.jmouse.ai.jpa.credential.CredentialProtection;
import org.jmouse.ai.jpa.entity.AiProviderSettings;

import java.util.List;

/**
 * Brings this application's stored provider keys up to the installation's credential protection, once,
 * before anything reads them.
 *
 * <h2>What one pass does</h2>
 *
 * <ul>
 *   <li><strong>With a credential key:</strong> seals every key still stored in the clear, re-seals
 *       every key in an older sealed format (the unbound {@code v1}) bound to its row, fills every
 *       missing fingerprint, and opens every sealed key once in its own row — so a changed key, or a
 *       sealed value moved between rows, is found at startup, by count, rather than by the first call
 *       that needed one.</li>
 *   <li><strong>Without one:</strong> refuses when any key is sealed, because nothing could read it and
 *       every call through it would fail later and less clearly. Otherwise fills missing fingerprints
 *       and reports how many keys are still in the clear, for the caller to warn about.</li>
 * </ul>
 *
 * <p>Running it again finds nothing to do, which is what makes it safe to run on every start.
 *
 * <h2>⚠️ Bulk updates, on purpose</h2>
 *
 * <p>Sealing a key changes how it is stored, not what is configured, so it must not move
 * {@code revision}: products record the revision a provider answered under as provenance, and a
 * revision that moved on startup would invalidate work that is still valid. Each update is also a
 * compare-and-set on the value it read, so two processes starting at once cannot seal one key twice or
 * overwrite a key somebody replaced in between.
 */
public final class ProviderCredentialUpgrade {

    /** What one pass found and changed. */
    public record Outcome(int sealedNow, int fingerprintedNow, int storedInTheClear, int alreadySealed) {
    }

    private final EntityManagerFactory entityManagerFactory;
    private final String               application;
    private final CredentialProtection credentialProtection;

    public ProviderCredentialUpgrade(
            EntityManagerFactory entityManagerFactory,
            String               application,
            CredentialProtection credentialProtection) {

        this.entityManagerFactory = entityManagerFactory;
        this.application          = application;
        this.credentialProtection = credentialProtection;
    }

    /**
     * @throws IllegalStateException when sealed keys exist and no credential key is configured, or when
     *                               the configured key cannot open them
     */
    public Outcome upgrade() {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            List<AiProviderSettings> keyed = keyedRows(entityManager);

            requireReadable(keyed);

            int sealedNow        = 0;
            int fingerprintedNow = 0;
            int storedInTheClear = 0;
            int alreadySealed    = 0;

            for (AiProviderSettings row : keyed) {
                String stored      = row.getApiKey();
                String context     = CredentialContexts.providerConfiguration(row.getId());
                String apiKey      = credentialProtection.reveal(stored, context);
                String fingerprint = CredentialFingerprint.of(row.getProvider(), apiKey);

                if (credentialProtection.needsSealing(stored)) {
                    sealedNow += replaceStored(entityManager, row, credentialProtection.protect(apiKey, context), fingerprint);
                    continue;
                }

                if (SecretCipher.isSealed(stored)) {
                    alreadySealed++;
                } else {
                    storedInTheClear++;
                }

                if (!fingerprint.equals(row.getApiKeyFingerprint())) {
                    fingerprintedNow += replaceStored(entityManager, row, stored, fingerprint);
                }
            }

            return new Outcome(sealedNow, fingerprintedNow, storedInTheClear, alreadySealed);
        });
    }

    private List<AiProviderSettings> keyedRows(EntityManager entityManager) {
        return entityManager.createQuery("""
                        select configured
                          from AiProviderSettings configured
                         where configured.application = :application
                           and configured.apiKey is not null
                           and configured.apiKey <> ''
                        """, AiProviderSettings.class)
                .setParameter("application", application)
                .getResultList();
    }

    /**
     * Refuses before anything is written, naming how many rows are affected and never which keys.
     */
    private void requireReadable(List<AiProviderSettings> keyed) {
        long sealed = keyed.stream().filter(row -> SecretCipher.isSealed(row.getApiKey())).count();

        if (sealed > 0 && !credentialProtection.isEncrypting()) {
            throw new IllegalStateException(
                    sealed + " provider key(s) of '" + application + "' are stored encrypted, and no credential "
                    + "key is configured to open them. Configure the credential key they were sealed with.");
        }

        long unreadable = keyed.stream()
                .filter(row -> SecretCipher.isSealed(row.getApiKey()))
                .filter(row -> !opens(row))
                .count();

        if (unreadable > 0) {
            throw new IllegalStateException(
                    unreadable + " provider key(s) of '" + application + "' cannot be opened with the configured "
                    + "credential key in the rows they are stored in: either the key is not the one they were "
                    + "sealed with, or a sealed value was moved from another row.");
        }
    }

    private boolean opens(AiProviderSettings row) {
        try {
            credentialProtection.reveal(row.getApiKey(), CredentialContexts.providerConfiguration(row.getId()));
            return true;
        } catch (IllegalStateException | IllegalArgumentException refusal) {
            return false;
        }
    }

    private static int replaceStored(EntityManager entityManager, AiProviderSettings row, String stored, String fingerprint) {
        return entityManager.createQuery("""
                        update AiProviderSettings configured
                           set configured.apiKey            = :stored,
                               configured.apiKeyFingerprint = :fingerprint
                         where configured.id     = :id
                           and configured.apiKey = :previous
                        """)
                .setParameter("stored", stored)
                .setParameter("fingerprint", fingerprint)
                .setParameter("id", row.getId())
                .setParameter("previous", row.getApiKey())
                .executeUpdate();
    }
}
