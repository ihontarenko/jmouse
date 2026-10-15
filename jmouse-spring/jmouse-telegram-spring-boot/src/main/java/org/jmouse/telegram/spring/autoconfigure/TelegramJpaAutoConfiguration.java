package org.jmouse.telegram.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.jpa.JpaIdentitySource;
import org.jmouse.telegram.jpa.JpaTelegramAccounts;
import org.jmouse.telegram.jpa.TelegramAccounts;
import org.jmouse.vault.seal.SecretCipher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * ⚙️ Identities as rows in {@code telegram_accounts}, administered from a screen.
 *
 * <p>⚠️ Ordered <strong>before</strong> {@link TelegramAutoConfiguration} so that this
 * {@link IdentitySource} is the one that exists — the properties-backed source there steps aside.
 *
 * <h2>⚠️ Opt-in, and sealed by the vault</h2>
 *
 * <p>Only with {@code jmouse.telegram.accounts.store=database}. Having a vault key is not a reason to
 * replace identities a product configured in properties — that used to be implied by setting a key, and
 * an installation turning on its vault would have lost every bot it had.
 *
 * <p>The credential column is sealed by the vault's {@link SecretCipher}, under the vault's keyring
 * ({@code jmouse.vault.active-key}); without one this configuration contributes nothing rather than
 * writing tokens in the clear.
 */
@AutoConfiguration(before = TelegramAutoConfiguration.class,
                   afterName = "org.jmouse.vault.spring.autoconfigure.VaultJpaAutoConfiguration")
@ConditionalOnClass({EntityManager.class, JpaIdentitySource.class})
@ConditionalOnProperty(name = "jmouse.telegram.accounts.store", havingValue = "database")
@ConditionalOnBean(SecretCipher.class)
public class TelegramJpaAutoConfiguration {

    /**
     * ⚙️ Reading and administering the account table.
     *
     * <p>⚠️ The {@link EntityManager} injected here is Spring's shared, transaction-aware proxy, so this
     * store joins whatever transaction the caller already has.
     */
    @Bean
    @ConditionalOnMissingBean(TelegramAccounts.class)
    public JpaTelegramAccounts telegramAccounts(EntityManager entityManager, SecretCipher cipher) {
        return new JpaTelegramAccounts(entityManager, cipher);
    }

    /** ⚙️ The reason the table exists: a rotated token takes effect on the next send. */
    @Bean
    @ConditionalOnMissingBean(IdentitySource.class)
    public IdentitySource telegramDatabaseIdentitySource(JpaTelegramAccounts accounts) {
        return new JpaIdentitySource(accounts);
    }
}
