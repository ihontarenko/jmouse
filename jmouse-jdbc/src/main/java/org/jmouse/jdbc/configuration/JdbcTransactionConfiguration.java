package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.*;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.transaction.JdbcTransactionCoordinator;
import org.jmouse.jdbc.transaction.JdbcTransactionSessionFactory;
import org.jmouse.transaction.TransactionManager;
import org.jmouse.transaction.configuration.TransactionInfrastructureConfiguration;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;
import org.jmouse.transaction.infrastructure.TransactionSessionFactory;
import org.jmouse.transaction.synchronization.SynchronizationContextHolder;

@BeanFactories
@BeanImport(TransactionInfrastructureConfiguration.class)
public class JdbcTransactionConfiguration {

    public static final String JDBC_TX_SESSION_FACTORY = "jdbcTransactionSessionFactory";
    public static final String JDBC_TX_MANAGER         = "jdbcTransactionManager";

    @Bean(JDBC_TX_SESSION_FACTORY)
    public TransactionSessionFactory jdbcTransactionSessionFactory(
            @Qualifier(ConnectionConfiguration.DATA_SOURCE_CONNECTION_PROVIDER) ConnectionProvider rawProvider
    ) {
        return new JdbcTransactionSessionFactory(rawProvider);
    }

    @Bean(JDBC_TX_MANAGER)
    @PrimaryBean
    public TransactionManager jdbcTransactionManager(
            TransactionContextHolder contextHolder,
            @Qualifier(JDBC_TX_SESSION_FACTORY) TransactionSessionFactory sessionFactory,
            SynchronizationContextHolder synchronizationHolder
    ) {
        return new JdbcTransactionCoordinator(contextHolder, sessionFactory, synchronizationHolder);
    }
}
