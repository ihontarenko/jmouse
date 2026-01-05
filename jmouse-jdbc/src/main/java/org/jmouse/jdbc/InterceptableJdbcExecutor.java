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

/**
 * {@link JdbcExecutor} implementation that supports interception via a
 * {@link Chain} of {@link JdbcExecutionContext execution steps}.
 * <p>
 * {@code InterceptableJdbcExecutor} acts as a <b>decorator</b> around a
 * concrete {@link JdbcExecutor} and exposes all JDBC calls as immutable
 * {@link JdbcCall} objects that can be intercepted, transformed, or short-circuited.
 *
 * <h3>Typical interception use cases</h3>
 * <ul>
 *     <li>SQL logging / tracing</li>
 *     <li>Metrics and timing</li>
 *     <li>Query guards and validation</li>
 *     <li>Transaction participation checks</li>
 *     <li>Retry or fail-fast policies</li>
 * </ul>
 *
 * <h3>Execution model</h3>
 * <pre>{@code
 * JdbcExecutor base = ...;
 * Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain = ...;
 *
 * JdbcExecutor executor = new InterceptableJdbcExecutor(base, chain);
 *
 * executor.execute(
 *     "select * from users",
 *     StatementCallback.QUERY,
 *     extractor
 * );
 * }</pre>
 *
 * <p>
 * Each invocation:
 * <ol>
 *     <li>Creates a fresh {@link JdbcExecutionContext}</li>
 *     <li>Wraps parameters into a specific {@link JdbcCall} implementation</li>
 *     <li>Delegates execution to the interceptor {@link Chain}</li>
 * </ol>
 *
 * @author jMouse
 */
public final class InterceptableJdbcExecutor implements JdbcExecutor {

    /**
     * Underlying executor performing the actual JDBC work.
     */
    private final JdbcExecutor delegate;

    /**
     * Interceptor chain applied to every JDBC call.
     */
    private final Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain;

    /**
     * Creates a new interceptable executor.
     *
     * @param delegate underlying JDBC executor
     * @param chain    interceptor chain
     */
    public InterceptableJdbcExecutor(JdbcExecutor delegate,
                                     Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain) {
        this.delegate = Contract.nonNull(delegate, "delegate");
        this.chain = Contract.nonNull(chain, "chain");
    }

    /**
     * Creates a new execution context for a single JDBC invocation.
     * <p>
     * A fresh context is created per call to ensure isolation between executions.
     *
     * @return new execution context
     */
    private JdbcExecutionContext newContext() {
        return new JdbcExecutionContext(delegate);
    }

    /**
     * Executes a query operation through the interceptor chain.
     *
     * @param sql        SQL query
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param callback   JDBC query callback
     * @param extractor  result set extractor
     * @param <T>        result type
     * @return extracted query result
     * @throws SQLException if execution fails
     */
    @Override
    public <T> T execute(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler handler,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcQueryCall<T>     call    = new JdbcQueryCall<>(sql, binder, configurer, handler, callback, extractor);
        @SuppressWarnings("unchecked")
        T result = (T) chain.run(context, call);
        return result;
    }

    /**
     * Executes an update operation (INSERT / UPDATE / DELETE) through the interceptor chain.
     *
     * @param sql        SQL statement
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param callback   update callback
     * @return update count
     * @throws SQLException if execution fails
     */
    @Override
    public int executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler handler,
            StatementCallback<Integer> callback
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcUpdateCall       call    = new JdbcUpdateCall(sql, binder, configurer, handler, callback);
        Object               result  = chain.run(context, call);
        return (Integer) result;
    }

    /**
     * Executes a batch update operation through the interceptor chain.
     *
     * @param sql        SQL statement
     * @param binders    parameter binders per batch item
     * @param configurer statement configuration
     * @param callback   batch callback
     * @return array of update counts
     * @throws SQLException if execution fails
     */
    @Override
    public int[] executeBatch(
            String sql,
            List<? extends StatementBinder> binders,
            StatementConfigurer configurer,
            StatementHandler handler,
            StatementCallback<int[]> callback
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcBatchUpdateCall  call    = new JdbcBatchUpdateCall(sql, binders, configurer, handler, callback);
        Object               result  = chain.run(context, call);
        return (int[]) result;
    }

    /**
     * Executes an update operation returning generated keys through the interceptor chain.
     *
     * @param sql        SQL statement
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param callback   key update callback
     * @param <K>        key/result type
     * @return extracted key result
     * @throws SQLException if execution fails
     */
    @Override
    public <K> K executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler handler,
            KeyUpdateCallback<K> callback
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcKeyUpdateCall<K> call    = new JdbcKeyUpdateCall<>(sql, binder, configurer, handler, callback);
        @SuppressWarnings("unchecked")
        K result = (K) chain.run(context, call);
        return result;
    }

    /**
     * Convenience overload delegating generated key extraction to a {@link KeyExtractor}.
     *
     * @param sql        SQL statement
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param extractor  key extractor
     * @param <K>        key/result type
     * @return extracted key result
     * @throws SQLException if execution fails
     */
    @Override
    public <K> K executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler handler,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return executeUpdate(sql, binder, configurer, handler, (statement, keys) -> extractor.extract(keys));
    }

    /**
     * Executes a callable statement (stored procedure or function) through the interceptor chain.
     *
     * @param sql        call SQL
     * @param binder     callable statement binder
     * @param configurer statement configuration
     * @param callback   callable callback
     * @param <T>        result type
     * @return callback result
     * @throws SQLException if execution fails
     */
    @Override
    public <T> T executeCall(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler handler,
            CallableCallback<T> callback
    ) throws SQLException {
        JdbcExecutionContext context = newContext();
        JdbcCallableCall<T>  call    = new JdbcCallableCall<>(sql, binder, configurer, handler, callback);
        @SuppressWarnings("unchecked")
        T result = (T) chain.run(context, call);
        return result;
    }
}
