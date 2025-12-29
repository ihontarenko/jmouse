package org.jmouse.transaction.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.PrimaryBean;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.transaction.*;

@BeanFactories
public class TransactionTemplateConfiguration {

    public static final String TX_CALLBACK            = "txCallback";
    public static final String TX_DEFINITION_DEFAULTS = "txDefinitionDefaults";
    public static final String TX_OPERATIONS          = "txOperations";

    @Bean(TX_CALLBACK)
    @PrimaryBean
    public TransactionCallback txCallback(TransactionManager coordinator) {
        return new TransactionTemplate(coordinator);
    }

    // optional: TxOperations convenience facade
    @Bean(TX_OPERATIONS)
    public TransactionOperations txOperations(
            TransactionCallback callback, @Qualifier(TX_DEFINITION_DEFAULTS) TransactionDefinition defaults) {
        return new TransactionOperations(callback, defaults);
    }

    @Bean(TX_DEFINITION_DEFAULTS)
    public TransactionDefinition txDefinitionDefaults() {
        return DefaultTransactionDefinition.builder()
                .propagation(TransactionPropagation.REQUIRED)
                .timeoutSeconds(30)
                .build();
    }

}