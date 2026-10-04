package org.jmouse.telegram.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.jpa.AesGcmCredentialCipher;
import org.jmouse.telegram.jpa.CredentialCipher;
import org.jmouse.telegram.jpa.JpaIdentitySource;
import org.jmouse.telegram.jpa.JpaTelegramAccounts;
import org.jmouse.telegram.jpa.JpaTelegramBindings;
import org.jmouse.telegram.jpa.TelegramAccounts;
import org.jmouse.telegram.jpa.TelegramBindings;
import org.jmouse.telegram.spring.TelegramSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * ⚙️ Identities out of the database, when {@code jmouse-telegram-jpa} is on the classpath.
 *
 * <p>⚠️ Ordered **before** {@link TelegramAutoConfiguration} so that this
 * {@link IdentitySource} is the one that exists — the properties-backed source there is
 * {@code @ConditionalOnMissingBean} and therefore steps aside. That direction is the point: adding the
 * persistence module is how an installation moves from configured identities to administered ones, and
 * it should not also have to switch something off.
 *
 * <h2>⚠️ It needs a key, and says so rather than storing tokens in the clear</h2>
 *
 * <p>{@code jmouse.telegram.credential-key} seals the credential column. Without it this configuration
 * refuses to contribute anything, so the application falls back to properties-configured identities
 * rather than quietly writing plaintext tokens into a database — which is a state nobody would choose
 * and an installation would reach by forgetting one line.
 *
 * <p>Generate one with {@code openssl rand -base64 32} and keep it in an environment variable. ⚠️
 * Rotating it means re-sealing every account; a changed key produces an {@link IllegalStateException}
 * per identity, which is the correct and visible failure.
 */
@AutoConfiguration(before = TelegramAutoConfiguration.class)
@ConditionalOnClass({EntityManager.class, JpaIdentitySource.class})
@ConditionalOnProperty(name = "jmouse.telegram.credential-key")
public class TelegramJpaAutoConfiguration {

    /**
     * ⚙️ The cipher, from the configured key.
     *
     * <p>⚠️ The absence of a key is handled by the <strong>class</strong> condition above, not here. A
     * {@code @Bean} method returning {@code null} would register a {@code NullBean}, and the next bean
     * that needs a {@link CredentialCipher} would fail to start the context — the opposite of stepping
     * aside. The condition makes the whole configuration vanish instead, so the properties-backed
     * identity source in {@link TelegramAutoConfiguration} answers and the application runs.
     */
    @Bean
    @ConditionalOnMissingBean
    public CredentialCipher telegramCredentialCipher(TelegramSettings settings) {
        return AesGcmCredentialCipher.fromBase64Key(settings.credentialKey());
    }

    /**
     * ⚙️ Reading and administering the account table.
     *
     * <p>⚠️ The {@link EntityManager} injected here is Spring's shared, transaction-aware proxy, so this
     * store joins whatever transaction the caller already has — which is the arrangement the library
     * requires, since it demarcates none of its own.
     */
    @Bean
    @ConditionalOnMissingBean(TelegramAccounts.class)
    public JpaTelegramAccounts telegramAccounts(EntityManager entityManager, CredentialCipher cipher) {
        return new JpaTelegramAccounts(entityManager, cipher);
    }

    /** ⚙️ The reason the table exists: a rotated token takes effect on the next send. */
    @Bean
    @ConditionalOnMissingBean(IdentitySource.class)
    public IdentitySource telegramDatabaseIdentitySource(JpaTelegramAccounts accounts) {
        return new JpaIdentitySource(accounts);
    }

    /**
     * ⚙️ Which chat reaches which of the product's subjects.
     *
     * <p>⚠️ The {@code /start} handler is deliberately <strong>not</strong> registered for you. Claiming
     * {@code /start} in every application that happens to have this jar would take a command products
     * routinely want for a greeting, an onboarding step or a menu. One line, where the product can see
     * it:
     *
     * <pre>{@code
     * dispatcher.onCommand("start", new BindingCommand(bindings, gateway));
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean(TelegramBindings.class)
    public JpaTelegramBindings telegramBindings(EntityManager entityManager) {
        return new JpaTelegramBindings(entityManager);
    }
}
