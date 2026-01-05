package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.ResultSet;

/**
 * {@link JdbcCall} descriptor for executing a query that produces a {@link ResultSet}.
 * <p>
 * This call type captures all components required for query execution:
 * <ul>
 *     <li>SQL text</li>
 *     <li>{@link StatementBinder} for parameter binding</li>
 *     <li>{@link StatementConfigurer} for statement tuning (timeouts, fetch size, etc.)</li>
 *     <li>{@link StatementCallback} that performs the JDBC execution and returns a {@link ResultSet}</li>
 *     <li>{@link ResultSetExtractor} that maps the {@link ResultSet} into the desired result type</li>
 * </ul>
 *
 * <h3>Immutability and "with" methods</h3>
 * <p>
 * Records are immutable; this type provides a set of {@code with(...)} methods
 * to produce modified copies (useful for interceptors that rewrite SQL, inject
 * configurers, swap extractors, etc.).
 *
 * <p>
 * For statement configuration, {@link #with(StatementConfigurer)} <b>combines</b>
 * the existing configurer with the provided one using
 * {@link StatementConfigurer#combine(StatementConfigurer, StatementConfigurer)}.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * JdbcQueryCall<List<User>> call = ...;
 *
 * call = call.with("select * from users where active = 1")
 *            .with(StatementConfigurer.timeout(5));
 * }</pre>
 *
 * @param <T> extracted result type
 *
 * @author jMouse
 */
public record JdbcQueryCall<T>(
        String sql,
        StatementBinder binder,
        StatementConfigurer configurer,
        StatementHandler handler,
        StatementCallback<ResultSet> callback,
        ResultSetExtractor<T> extractor
) implements JdbcCall<T> {

    /**
     * Returns the logical operation type for this call.
     *
     * @return {@link JdbcOperation#QUERY}
     */
    @Override
    public JdbcOperation operation() {
        return JdbcOperation.QUERY;
    }

    /**
     * Returns a new call instance with an additional {@link StatementConfigurer}.
     * <p>
     * The provided configurer is combined with the existing one.
     *
     * @param configurer additional statement configuration
     * @return new call instance with combined configuration
     */
    public JdbcQueryCall<T> with(StatementConfigurer configurer) {
        return new JdbcQueryCall<>(
                sql, binder,
                StatementConfigurer.combine(this.configurer(), configurer),
                handler,
                callback, extractor
        );
    }

    /**
     * Returns a new call instance with a replaced {@link StatementCallback}.
     *
     * @param callback new callback
     * @return new call instance
     */
    public JdbcQueryCall<T> with(StatementCallback<ResultSet> callback) {
        return new JdbcQueryCall<>(sql, binder, configurer, handler, callback, extractor);
    }

    /**
     * Returns a new call instance with a replaced {@link ResultSetExtractor}.
     *
     * @param extractor new extractor
     * @return new call instance
     */
    public JdbcQueryCall<T> with(ResultSetExtractor<T> extractor) {
        return new JdbcQueryCall<>(sql, binder, configurer, handler, callback, extractor);
    }

    /**
     * Returns a new call instance with a replaced {@link StatementBinder}.
     *
     * @param binder new binder
     * @return new call instance
     */
    public JdbcQueryCall<T> with(StatementBinder binder) {
        return new JdbcQueryCall<>(sql, binder, configurer, handler, callback, extractor);
    }

    /**
     * Returns a new call instance with replaced SQL text.
     *
     * @param sql new SQL
     * @return new call instance
     */
    public JdbcQueryCall<T> with(String sql) {
        return new JdbcQueryCall<>(sql, binder, configurer, handler, callback, extractor);
    }

}
