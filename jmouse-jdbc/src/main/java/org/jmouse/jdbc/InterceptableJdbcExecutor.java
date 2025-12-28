package org.jmouse.jdbc;

import org.jmouse.core.Contract;
import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.intercept.*;
import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class InterceptableJdbcExecutor implements JdbcExecutor {

    private final JdbcExecutor delegate;
    private final Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain;

    public InterceptableJdbcExecutor(JdbcExecutor delegate,
                                     Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain) {
        this.delegate = Contract.nonNull(delegate, "delegate");
        this.chain = Contract.nonNull(chain, "chain");
    }

    private JdbcExecutionContext newContext() {
        return new JdbcExecutionContext(delegate);
    }

    @Override
    public <T> T execute(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcQueryCall<T>     call    = new JdbcQueryCall<>(sql, binder, configurer, callback, extractor);
        @SuppressWarnings("unchecked") T result = (T) chain.run(context, call);
        return result;
    }

    @Override
    public int executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            StatementCallback<Integer> callback
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcUpdateCall       call    = new JdbcUpdateCall(sql, binder, configurer, callback);
        Object               result  = chain.run(context, call);
        return (Integer) result;
    }

    @Override
    public int[] executeBatch(
            String sql,
            List<? extends PreparedStatementBinder> binders,
            StatementConfigurer configurer,
            StatementCallback<int[]> callback
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcBatchUpdateCall  call    = new JdbcBatchUpdateCall(sql, binders, configurer, callback);
        Object               result  = chain.run(context, call);
        return (int[]) result;
    }

    @Override
    public <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            KeyUpdateCallback<K> callback
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcKeyUpdateCall<K> call    = new JdbcKeyUpdateCall<>(sql, binder, configurer, callback);
        @SuppressWarnings("unchecked")
        K result = (K) chain.run(context, call);
        return result;
    }

    @Override
    public <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return executeUpdate(sql, binder, configurer, (statement, keys) -> extractor.extract(keys));
    }

    @Override
    public <T> T executeCall(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            CallableCallback<T> callback
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcCallableCall<T>  call    = new JdbcCallableCall<>(sql, binder, configurer, callback);
        @SuppressWarnings("unchecked") T result = (T) chain.run(context, call);
        return result;
    }
}
