package org.jmouse.ai.spring;

import org.jmouse.ai.jpa.ProviderCredentialUpgrade;
import org.jmouse.ai.jpa.credential.CredentialProtection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Runs the provider credential upgrade once every bean exists — after the schema migrations, before
 * the web server, schedulers or workers start — and decides what the outcome means for this start.
 *
 * <ul>
 *   <li>Sealed keys and no vault keyring ({@code jmouse.vault.keys}), or one that cannot open them:
 *       <strong>the application refuses to start.</strong> Starting would only move the failure to the
 *       first call through each of those configurations, where it reads as a revoked key.</li>
 *   <li>No key and keys stored in the clear: it starts, and says so at warning level on every start.</li>
 *   <li>A key: legacy plain keys are sealed now, and the count is logged. Never a key, never a name.</li>
 * </ul>
 */
public class ProviderCredentialStartupCheck implements SmartInitializingSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderCredentialStartupCheck.class);

    private final ProviderCredentialUpgrade upgrade;
    private final CredentialProtection      credentialProtection;

    public ProviderCredentialStartupCheck(ProviderCredentialUpgrade upgrade, CredentialProtection credentialProtection) {
        this.upgrade              = upgrade;
        this.credentialProtection = credentialProtection;
    }

    @Override
    public void afterSingletonsInstantiated() {
        ProviderCredentialUpgrade.Outcome outcome;

        try {
            outcome = upgrade.upgrade();
        } catch (IllegalStateException refusal) {
            throw new IllegalStateException(refusal.getMessage()
                    + " Configure the vault keyring (jmouse.vault.keys, jmouse.vault.active-key) with the key they were sealed with.", refusal);
        }

        if (credentialProtection.isEncrypting()) {
            LOGGER.info("jMouse AI provider keys are encrypted at rest: {} sealed now, {} already sealed, "
                      + "{} fingerprint(s) refreshed.",
                    outcome.sealedNow(), outcome.alreadySealed(), outcome.fingerprintedNow());
            return;
        }

        LOGGER.warn("⚠️ jMouse AI has no credential key, so provider keys are stored IN THE CLEAR — {} stored "
                  + "that way now, and every new one will be too. Anyone who can read the database or a "
                  + "backup of it can read them. Configure the vault (jmouse.vault.active-key and jmouse.vault.keys, base64 of 32 random bytes) "
                  + "(`openssl rand -base64 32`); existing keys are sealed on the next start.",
                outcome.storedInTheClear());
    }
}
