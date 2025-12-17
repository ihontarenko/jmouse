package org.jmouse.jdbc.core;

import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.JdbcOperation;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public final class ChainedJdbcExecutor implements JdbcExecutor {

    private static final PreparedStatementBinder NO_BINDER = stmt -> {};

    private final JdbcExecutor                                     delegate;
    private final Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain;

    public ChainedJdbcExecutor(
            JdbcExecutor delegate,
            Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.chain = Objects.requireNonNull(chain, "chain");
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

        JdbcExecutionContext ctx = new JdbcExecutionContext(delegate);

        JdbcCall<T> call = new JdbcCall<>(
                sql,
                binder,
                statementCallback,
                extractor,
                JdbcOperation.QUERY
        );

        try {
            return (T) chain.run(ctx, call);
        } catch (RuntimeException e) {
            // SQLException contract: якщо хочеш – тут можна зробити unwrapping/wrapping пізніше
            throw e;
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return executeUpdate(sql, NO_BINDER);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int executeUpdate(String sql, PreparedStatementBinder binder) throws SQLException {

        JdbcExecutionContext ctx = new JdbcExecutionContext(delegate);

        JdbcCall<Integer> call = new JdbcCall<>(
                sql,
                binder,
                null,
                null,
                JdbcOperation.UPDATE
        );

        Object result = chain.run(ctx, call);
        return (Integer) result;
    }
}
