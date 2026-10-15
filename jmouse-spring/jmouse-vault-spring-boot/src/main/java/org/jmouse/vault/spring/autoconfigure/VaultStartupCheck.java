package org.jmouse.vault.spring.autoconfigure;

import org.jmouse.vault.jpa.JpaVaultSource;
import org.jmouse.vault.seal.AesGcmSecretCipher;
import org.jmouse.vault.seal.SecretCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;

/**
 * 🚦 Refuses to start with secrets sealed under a key that is not configured — every one of them would
 * fail at first use, one call at a time, looking like a revoked credential — and finishes a rotation by
 * resealing rows that are not under the active key.
 */
public final class VaultStartupCheck implements SmartInitializingSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultStartupCheck.class);

    private static final String EXTERNAL_KEY = "external";

    private final JpaVaultSource        source;
    private final SecretCipher          cipher;
    private final TransactionOperations transactions;
    private final boolean               resealOnStart;

    public VaultStartupCheck(JpaVaultSource source, SecretCipher cipher, TransactionOperations transactions,
                             boolean resealOnStart) {
        this.source        = source;
        this.cipher        = cipher;
        this.transactions  = transactions;
        this.resealOnStart = resealOnStart;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (cipher instanceof AesGcmSecretCipher aesGcm) {
            List<String> unknown = transactions.execute(status -> source.keyIdentifiersInUse()).stream()
                    .filter(key -> !EXTERNAL_KEY.equals(key) && !aesGcm.keyring().contains(key))
                    .toList();

            if (!unknown.isEmpty()) {
                throw new IllegalStateException(("Vault secrets are sealed with key(s) %s, which jmouse.vault.keys does "
                        + "not configure (configured: %s). Add the key back, or remove those secrets.")
                        .formatted(unknown, aesGcm.keyring().keyIdentifiers()));
            }
        }

        if (!resealOnStart) {
            return;
        }

        Integer resealed = transactions.execute(status -> source.resealOutdated());

        if (resealed != null && resealed > 0) {
            LOGGER.info("🔁 Resealed {} vault secret(s) under the active key.", resealed);
        }
    }
}
