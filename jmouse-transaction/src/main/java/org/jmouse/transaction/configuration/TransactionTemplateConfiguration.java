package org.jmouse.transaction.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.PrimaryBean;
import org.jmouse.tx.core.TransactionManager;

@BeanFactories
public class TransactionTemplateConfiguration {

    @Bean
    @PrimaryBean
    public TransactionCallback transactionCallback(TransactionManager txManager) {
        return new DefaultTransactionCallback(txManager);
    }

    // optional: TxOperations convenience facade
    @Bean
    public TxOperations txOperations(TransactionCallback callback) {
        return new TxOperations(callback, DefaultTransactionDefinition.defaults());
    }
}