package org.jmouse.telegram.management;

import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.administration.ChatAdministration;
import org.jmouse.telegram.journal.TelegramJournal;
import org.jmouse.telegram.jpa.TelegramAccounts;
import org.jmouse.telegram.jpa.TelegramBindings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The Telegram administration API, mounted by the product:
 *
 * <pre>{@code
 * @Import(TelegramManagementConfiguration.class)
 * }</pre>
 *
 * <p>⚠️ <strong>Imported, never auto-configured.</strong> A management surface switched on by being on the
 * classpath is a surface published by a dependency bump — and it has no gate of its own (see the package
 * note). Mounting it is a line somebody writes beside the rule that protects it.
 */
@Configuration(proxyBeanMethods = false)
public class TelegramManagementConfiguration {

    @Bean
    public TelegramAccountController telegramAccountController(
            ObjectProvider<TelegramAccounts> accounts, TelegramGateway gateway,
            ObjectProvider<PlatformTransactionManager> transactionManager) {
        return new TelegramAccountController(accounts, gateway, transactionsOf(transactionManager));
    }

    @Bean
    public TelegramBindingController telegramBindingController(
            ObjectProvider<TelegramBindings> bindings, ObjectProvider<PlatformTransactionManager> transactionManager) {
        return new TelegramBindingController(bindings, transactionsOf(transactionManager));
    }

    /**
     * Writes to accounts and bindings run in a transaction this module opens — through the product's
     * transaction manager, since the stores join whatever transaction is current.
     *
     * <p>⚠️ <strong>Built here, never published as a bean.</strong> It was a {@code TransactionOperations}
     * bean, and Boot's own {@code TransactionTemplate} is conditional on there being none — so mounting this
     * module silently removed the product's {@code TransactionTemplate}, and the product failed to start on
     * a service that had nothing to do with Telegram.
     */
    private static TransactionOperations transactionsOf(ObjectProvider<PlatformTransactionManager> manager) {
        PlatformTransactionManager available = manager.getIfAvailable();

        return available == null ? TransactionOperations.withoutTransaction() : new TransactionTemplate(available);
    }

    @Bean
    public TelegramJournalController telegramJournalController(TelegramJournal journal) {
        return new TelegramJournalController(journal);
    }

    @Bean
    public TelegramChatController telegramChatController(ChatAdministration administration, IdentitySource identities) {
        return new TelegramChatController(administration, identities);
    }

    @Bean
    public TelegramManagementProblems telegramManagementProblems() {
        return new TelegramManagementProblems();
    }
}
