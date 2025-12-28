package org.jmouse.jdbc;

import org.jmouse.core.Contract;
import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.intercept.*;
import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.CallableCallback;
import org.jmouse.jdbc.statement.CallableStatementBinder;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class InterceptableJdbcExecutor implements JdbcExecutor {

    private static final PreparedStatementBinder NO_BINDER = statement -> {
    };

    private final JdbcExecutor                                     delegate;
    private final Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain;

    public InterceptableJdbcExecutor(JdbcExecutor delegate, Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain) {
        this.delegate = Contract.nonNull(delegate, "delegate");
        this.chain = Contract.nonNull(chain, "chain");
    }

    protected JdbcExecutionContext newContext() {
        return new JdbcExecutionContext(delegate);
    }

    @Override
    public <T> T execute(
            String sql, StatementCallback<ResultSet> statementCallback, ResultSetExtractor<T> extractor
    ) throws SQLException {
        return execute(sql, NO_BINDER, statementCallback, extractor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(
            String sql, PreparedStatementBinder binder, StatementCallback<ResultSet> callback, ResultSetExtractor<T> extractor
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcQueryCall<T>     call    = new JdbcQueryCall<>(sql, binder, callback, extractor);
        return (T) chain.run(context, call);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return executeUpdate(sql, NO_BINDER);
    }

    @Override
    public int executeUpdate(String sql, PreparedStatementBinder binder) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcUpdateCall       call    = new JdbcUpdateCall(sql, binder);
        Object               result  = chain.run(context, call);
        return (Integer) result;
    }

    @Override
    public int[] executeBatch(String sql, List<? extends PreparedStatementBinder> binders) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcBatchUpdateCall  call    = new JdbcBatchUpdateCall(sql, binders);
        Object               result  = chain.run(context, call);
        return (int[]) result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> K executeUpdate(
            String sql, PreparedStatementBinder binder, KeyExtractor<K> extractor
    ) throws SQLException {
        JdbcExecutionContext context    = newContext();
        JdbcKeyUpdateCall<K> call   = new JdbcKeyUpdateCall<>(sql, binder, extractor);
        Object               result = chain.run(context, call);
        return (K) result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeCall(String sql, CallableStatementBinder binder, CallableCallback<T> callback) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcCallableCall<T>  call    = new JdbcCallableCall<>(sql, binder, callback);
        return (T) chain.run(context, call);
    }

}
