package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.*;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.transaction.*;
import org.jmouse.transaction.TransactionManager;
import org.jmouse.transaction.configuration.TransactionInfrastructureConfiguration;
import org.jmouse.transaction.infrastructure.JoinTransactionValidator;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;
import org.jmouse.transaction.infrastructure.TransactionSessionFactory;
import org.jmouse.transaction.synchronization.SynchronizationContextHolder;

import java.util.List;

/**
 * 🧩 JDBC-based transaction configuration.
 *
 * <p>
 * Registers core JDBC transaction infrastructure including:
 * </p>
 * <ul>
 *     <li>{@link TransactionSessionFactory} backed by JDBC connections</li>
 *     <li>{@link TransactionManager} implementation for coordinating transactions</li>
 *     <li>{@link ConnectionCustomizer} chain for connection-level tuning</li>
 * </ul>
 *
 * <p>
 * This configuration builds on top of {@link TransactionInfrastructureConfiguration}
 * which provides base transaction abstractions (context holders, synchronization, etc).
 * </p>
 *
 * <p>
 * ⚙️ Typical wiring:
 * </p>
 * <pre>{@code
 * DataSourceConnectionProvider (raw)
 *        ↓
 * JdbcTransactionSessionFactory
 *        ↓
 * JdbcTransactionCoordinator (TransactionManager)
 * }</pre>
 *
 * <p>
 * 💡 Customization:
 * </p>
 * <ul>
 *     <li>Extend {@link ConnectionCustomizer} chain to modify connection behavior</li>
 *     <li>Replace {@link ConnectionProvider} for custom data sources</li>
 * </ul>
 */
@BeanFactories
@BeanImport(TransactionInfrastructureConfiguration.class)
public class JdbcTransactionConfiguration {

    /**
     * 🔑 Bean name for {@link ConnectionCustomizer}.
     */
    public static final String CONNECTION_CUSTOMIZER   = "connectionCustomizer";

    /**
     * 🔑 Bean name for {@link TransactionSessionFactory}.
     */
    public static final String JDBC_TX_SESSION_FACTORY = "jdbcTransactionSessionFactory";

    /**
     * 🔑 Bean name for {@link TransactionManager}.
     */
    public static final String JDBC_TX_MANAGER         = "jdbcTransactionManager";

    /**
     * 🏭 Creates {@link TransactionSessionFactory} backed by JDBC.
     *
     * <p>
     * Uses a raw {@link ConnectionProvider} and applies a {@link ConnectionCustomizer}
     * chain before connections are used in transactional sessions.
     * </p>
     *
     * @param rawProvider JDBC connection provider (non-transactional)
     * @param customizer  connection customization strategy
     * @return JDBC-based {@link TransactionSessionFactory}
     */
    @Bean(JDBC_TX_SESSION_FACTORY)
    public TransactionSessionFactory jdbcTransactionSessionFactory(
            @Qualifier(JdbcConnectionProviderConfiguration.PRIMARY_CONNECTION_PROVIDER) ConnectionProvider rawProvider,
            @Qualifier(CONNECTION_CUSTOMIZER) ConnectionCustomizer customizer
    ) {
        return new JdbcTransactionSessionFactory(rawProvider, customizer);
    }

    /**
     * 🔧 Builds a composite {@link ConnectionCustomizer}.
     *
     * <p>
     * Combines multiple customization strategies into a single chain.
     * By default, applies:
     * </p>
     * <ul>
     *     <li>{@link IsolationReadOnlyConnectionCustomizer}</li>
     * </ul>
     *
     * @return composed {@link ConnectionCustomizer}
     */
    @Bean(CONNECTION_CUSTOMIZER)
    public ConnectionCustomizer connectionCustomizer() {
        return new CompositeConnectionCustomizer(List.of(
                new IsolationReadOnlyConnectionCustomizer()
        ));
    }

    /**
     * 🚦 Creates the primary JDBC {@link TransactionManager}.
     *
     * <p>
     * Coordinates transaction lifecycle:
     * </p>
     * <ul>
     *     <li>binds transactional context via {@link TransactionContextHolder}</li>
     *     <li>obtains sessions from {@link TransactionSessionFactory}</li>
     *     <li>manages synchronization callbacks via {@link SynchronizationContextHolder}</li>
     *     <li>validates join semantics via {@link JoinTransactionValidator}</li>
     * </ul>
     *
     * <p>
     * Marked as {@link PrimaryBean} to be used as the default
     * {@link TransactionManager} in the context.
     * </p>
     *
     * @param contextHolder           transaction context holder
     * @param sessionFactory          JDBC session factory
     * @param synchronizationHolder   synchronization context holder
     * @param joinTransactionValidator validator for transaction joining rules
     * @return JDBC-based {@link TransactionManager}
     */
    @Bean(JDBC_TX_MANAGER)
    @PrimaryBean
    public TransactionManager jdbcTransactionManager(
            TransactionContextHolder contextHolder,
            @Qualifier(JDBC_TX_SESSION_FACTORY) TransactionSessionFactory sessionFactory,
            SynchronizationContextHolder synchronizationHolder,
            JoinTransactionValidator joinTransactionValidator
    ) {
        return new JdbcTransactionCoordinator(
                contextHolder, sessionFactory, synchronizationHolder, joinTransactionValidator
        );
    }
}