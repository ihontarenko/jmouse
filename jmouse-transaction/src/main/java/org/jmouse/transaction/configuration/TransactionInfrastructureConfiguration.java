package org.jmouse.transaction.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import org.jmouse.transaction.infrastructure.thread.ThreadBoundTransactionContextHolder;
import org.jmouse.transaction.synchronization.SynchronizationContextHolder;
import org.jmouse.transaction.synchronization.ThreadBoundSynchronizationContextHolder;

@BeanFactories
public class TransactionInfrastructureConfiguration {

    public static final String TX_CONTEXT_HOLDER = "txContextHolder";
    public static final String TX_SYNC_HOLDER    = "txSynchronizationHolder";

    @Bean(TX_CONTEXT_HOLDER)
    public TransactionContextHolder transactionContextHolder() {
        TransactionContextHolder holder = new ThreadBoundTransactionContextHolder();
        TransactionContextAccessSupport.register(holder);
        return holder;
    }

    @Bean(TX_SYNC_HOLDER)
    public SynchronizationContextHolder synchronizationContextHolder() {
        return new ThreadBoundSynchronizationContextHolder();
    }

}
