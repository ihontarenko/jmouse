package org.jmouse.vault.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.vault.audit.VaultAudit;
import org.jmouse.vault.jpa.JpaCredentialSource;
import org.jmouse.vault.jpa.JpaVaultAudit;
import org.jmouse.vault.jpa.JpaVaultSource;
import org.jmouse.vault.seal.AesGcmSecretCipher;
import org.jmouse.vault.seal.SecretCipher;
import org.jmouse.vault.seal.VaultKeyring;
import org.jmouse.vault.spring.VaultSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * 🗄️ The database source, its credential definitions and its audit trail.
 *
 * <p>⚠️ Only with {@code jmouse.vault.active-key} set: a database source without a key would have to
 * store secrets in the clear, and there is deliberately no such mode.
 */
@AutoConfiguration(before = VaultAutoConfiguration.class)
@ConditionalOnClass({EntityManager.class, JpaVaultSource.class})
@ConditionalOnProperty(name = "jmouse.vault.active-key")
@EnableConfigurationProperties(VaultSettings.class)
public class VaultJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecretCipher vaultSecretCipher(VaultSettings settings) {
        VaultKeyring keyring = VaultKeyring.fromBase64(settings.keys(), settings.activeKey());

        if (keyring == null) {
            throw new IllegalStateException(("jmouse.vault.active-key is '%s' but no key under jmouse.vault.keys has "
                    + "a value — is the environment variable behind it set?").formatted(settings.activeKey()));
        }

        return new AesGcmSecretCipher(keyring);
    }

    @Bean
    @ConditionalOnMissingBean
    public JpaVaultSource vaultDatabaseSource(EntityManager entityManager, SecretCipher cipher) {
        return new JpaVaultSource(entityManager, cipher, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public JpaCredentialSource vaultDatabaseCredentials(EntityManager entityManager) {
        return new JpaCredentialSource(entityManager, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean(VaultAudit.class)
    public JpaVaultAudit vaultAudit(EntityManager entityManager) {
        return new JpaVaultAudit(entityManager);
    }

    @Bean
    public VaultStartupCheck vaultStartupCheck(JpaVaultSource source, SecretCipher cipher, VaultSettings settings,
                                               ObjectProvider<PlatformTransactionManager> transactionManager) {
        PlatformTransactionManager manager      = transactionManager.getIfAvailable();
        TransactionOperations      transactions = manager == null
                ? TransactionOperations.withoutTransaction()
                : new TransactionTemplate(manager);

        return new VaultStartupCheck(source, cipher, transactions, settings.resealOnStart());
    }
}
