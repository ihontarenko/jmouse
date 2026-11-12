package org.jmouse.jdbc;

import java.util.Objects;
import javax.sql.DataSource;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.connection.DataSourceConnectionProvider;
import org.jmouse.jdbc.connection.TransactionAwareConnectionProvider;
import org.jmouse.jdbc.parameters.MissingParameterPolicy;
import org.jmouse.jdbc.parameters.SQLParameterProcessor;
import org.jmouse.jdbc.parameters.compile.SQLPlanCompiler;
import org.jmouse.jdbc.parameters.lexer.SQLParameterSplitter;
import org.jmouse.jdbc.parameters.lexer.SQLParameterTokenizer;
import org.jmouse.jdbc.parameters.named.NamedSqlPreparedExecutionFactory;
import org.jmouse.jdbc.transaction.IsolationReadOnlyConnectionCustomizer;
import org.jmouse.jdbc.transaction.JdbcTransactionCoordinator;
import org.jmouse.jdbc.transaction.JdbcTransactionSessionFactory;
import org.jmouse.transaction.TransactionTemplate;
import org.jmouse.transaction.infrastructure.JoinAttributePolicy;
import org.jmouse.transaction.infrastructure.JoinTransactionValidator;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import org.jmouse.transaction.synchronization.SynchronizationContextHolder;
import org.jmouse.transaction.synchronization.ThreadBoundSynchronizationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Programmatic JDBC composition without an application or DI container.
 * The caller owns the supplied DataSource and its pool lifecycle.
 * Transactions are synchronous, thread-bound and local to that DataSource.
 */
public final class JdbcAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAccess.class);
    private final JdbcTemplate jdbc;
    private final NamedTemplate named;
    private final TransactionTemplate transactions;

    private JdbcAccess(ConnectionProvider raw) {
        this(raw, TransactionContextAccessSupport.current(), new ThreadBoundSynchronizationContextHolder());
    }

    private JdbcAccess(
        ConnectionProvider raw,
        TransactionContextHolder contexts,
        SynchronizationContextHolder synchronizations
    ) {
        Objects.requireNonNull(raw, "provider");
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(synchronizations, "synchronizations");
        JdbcExecutor executor = new SQLExecutor(
            new TransactionAwareConnectionProvider(raw, contexts),
            contexts
        );
        jdbc = new JdbcTemplate.Default(executor);
        SQLParameterProcessor processor = new SQLParameterProcessor(
            new SQLParameterTokenizer(new SQLParameterSplitter()),
            new SQLPlanCompiler()
        );
        named = new NamedTemplate.Default(
            executor,
            new NamedSqlPreparedExecutionFactory(processor, MissingParameterPolicy.FAIL_FAST)
        );
        JoinTransactionValidator validator = new JoinTransactionValidator(
            JoinAttributePolicy.FAIL,
            JoinAttributePolicy.FAIL,
            JoinAttributePolicy.FAIL,
            LOGGER::warn
        );
        transactions = new TransactionTemplate(
            new JdbcTransactionCoordinator(
                contexts,
                new JdbcTransactionSessionFactory(raw, new IsolationReadOnlyConnectionCustomizer(), contexts),
                synchronizations,
                validator
            )
        );
    }

    public static JdbcAccess from(DataSource dataSource) {
        return using(new DataSourceConnectionProvider(dataSource));
    }

    /** Custom acquisition/release strategies, including routing; the caller owns their lifecycle. */
    public static JdbcAccess using(ConnectionProvider provider) {
        return new JdbcAccess(provider);
    }

    /** Explicit scope strategies for integrations that do not use the global compatibility bridge. */
    public static JdbcAccess using(
        ConnectionProvider provider,
        TransactionContextHolder contexts,
        SynchronizationContextHolder synchronizations
    ) {
        return new JdbcAccess(provider, contexts, synchronizations);
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    public NamedTemplate named() {
        return named;
    }

    public TransactionTemplate transactions() {
        return transactions;
    }
}
