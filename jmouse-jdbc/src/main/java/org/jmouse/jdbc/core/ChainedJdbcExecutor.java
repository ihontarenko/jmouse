package org.jmouse.jdbc.core;

import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.core.exception.JdbcAccessException;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.JdbcOperation;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class ChainedJdbcExecutor implements JdbcExecutor {

    private static final PreparedStatementBinder NO_BINDER = statement -> {};

    private final JdbcExecutor                                     delegate;
    private final Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain;

    public ChainedJdbcExecutor(
            JdbcExecutor delegate,
            Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain
    ) {
        this.delegate = delegate;
        this.chain = chain;
    }

    @Override
    public <T> T execute(
            String sql,
            StatementCallback<ResultSet> statementCallback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return execute(sql, NO_BINDER, statementCallback, extractor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(
            String sql,
            PreparedStatementBinder binder,
            StatementCallback<ResultSet> statementCallback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {

        JdbcExecutionContext context = newContext();
        JdbcCall<T>          call    = new JdbcCall<>(
                sql,
                binder,
                statementCallback,
                extractor,
                JdbcOperation.QUERY
        );

        try {
            return (T) chain.run(context, call);
        } catch (JdbcAccessException e) {
            throw e.getCause();
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return executeUpdate(sql, NO_BINDER);
    }

    @Override
    public int executeUpdate(String sql, PreparedStatementBinder binder) throws SQLException {

        JdbcExecutionContext ctx = newContext();

        JdbcCall<Integer> call = new JdbcCall<>(
                sql,
                binder,
                null,
                null,
                JdbcOperation.UPDATE
        );

        try {
            return (Integer) chain.run(ctx, call);
        } catch (JdbcAccessException e) {
            throw e.getCause();
        }
    }

    /**
     * Factory method for context customization (dialect, settings, etc).
     */
    protected JdbcExecutionContext newContext() {
        return new JdbcExecutionContext(delegate, null);
    }
}
