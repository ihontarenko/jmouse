package org.jmouse.jdbc;

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
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import org.jmouse.transaction.synchronization.ThreadBoundSynchronizationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Objects;

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

    private JdbcAccess(DataSource dataSource) {
        DataSourceConnectionProvider raw = new DataSourceConnectionProvider(
                Objects.requireNonNull(dataSource, "dataSource"));
        JdbcExecutor executor = new SQLExecutor(new TransactionAwareConnectionProvider(raw));
        jdbc = new JdbcTemplate.Default(executor);
        SQLParameterProcessor processor = new SQLParameterProcessor(
                new SQLParameterTokenizer(new SQLParameterSplitter()), new SQLPlanCompiler());
        named = new NamedTemplate.Default(executor,
                new NamedSqlPreparedExecutionFactory(processor, MissingParameterPolicy.FAIL_FAST));
        JoinTransactionValidator validator = new JoinTransactionValidator(
                JoinAttributePolicy.FAIL, JoinAttributePolicy.FAIL, JoinAttributePolicy.FAIL, LOGGER::warn);
        transactions = new TransactionTemplate(new JdbcTransactionCoordinator(
                TransactionContextAccessSupport.current(),
                new JdbcTransactionSessionFactory(raw, new IsolationReadOnlyConnectionCustomizer()),
                new ThreadBoundSynchronizationContextHolder(), validator));
    }

    public static JdbcAccess from(DataSource dataSource) {
        return new JdbcAccess(dataSource);
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
