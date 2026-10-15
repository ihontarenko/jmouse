package org.jmouse.vault.management;

import org.jmouse.vault.Vault;
import org.jmouse.vault.audit.VaultAudit;
import org.jmouse.vault.credential.Credentials;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * 🔌 The vault's management API. Imported by a product, never auto-configured — an endpoint that can show
 * a secret appears because somebody typed {@code @Import}, and the same product then gates it:
 *
 * <pre>{@code
 * ExternalAccessRules.builder()
 *         .type(VaultReadController.class,   Declaration.permission("vault:read").atScope("GLOBAL"))
 *         .type(VaultWriteController.class,  Declaration.permission("vault:write").atScope("GLOBAL"))
 *         .type(VaultRevealController.class, Declaration.permission("vault:reveal").atScope("GLOBAL"))
 *         .build();
 * }</pre>
 *
 * <p>⚠️ The transaction template is built here and never published, so mounting this does not replace a
 * product's own {@code TransactionTemplate} (the {@code JMF-366} trap).
 */
@Configuration(proxyBeanMethods = false)
public class VaultManagementConfiguration {

    @Bean
    public VaultReadController vaultReadController(Vault vault, Credentials credentials, VaultAudit audit,
                                                   ObjectProvider<PlatformTransactionManager> transactionManager) {
        return new VaultReadController(vault, credentials, audit, transactionsOf(transactionManager));
    }

    @Bean
    public VaultWriteController vaultWriteController(Vault vault, Credentials credentials, VaultAudit audit,
                                                     ObjectProvider<PlatformTransactionManager> transactionManager,
                                                     ObjectProvider<VaultActors> actors) {
        return new VaultWriteController(vault, credentials, audit, transactionsOf(transactionManager), Clock.systemUTC(),
                actors.getIfAvailable(VaultActors::principalNames));
    }

    @Bean
    public VaultRevealController vaultRevealController(Vault vault, VaultAudit audit,
                                                       ObjectProvider<PlatformTransactionManager> transactionManager,
                                                       ObjectProvider<VaultActors> actors) {
        return new VaultRevealController(vault, audit, transactionsOf(transactionManager), Clock.systemUTC(),
                actors.getIfAvailable(VaultActors::principalNames));
    }

    @Bean
    public VaultManagementProblems vaultManagementProblems() {
        return new VaultManagementProblems();
    }

    private static TransactionOperations transactionsOf(ObjectProvider<PlatformTransactionManager> manager) {
        PlatformTransactionManager available = manager.getIfAvailable();

        return available == null ? TransactionOperations.withoutTransaction() : new TransactionTemplate(available);
    }
}
