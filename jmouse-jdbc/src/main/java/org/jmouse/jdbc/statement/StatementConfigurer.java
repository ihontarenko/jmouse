package org.jmouse.jdbc.statement;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Functional contract for configuring a JDBC {@link Statement} prior to execution.
 * <p>
 * {@code StatementConfigurer} is used to apply non-functional, execution-related
 * settings such as:
 * <ul>
 *     <li>query timeout</li>
 *     <li>fetch size</li>
 *     <li>max rows</li>
 *     <li>driver-specific hints</li>
 * </ul>
 *
 * <p>
 * It is intentionally separated from parameter binding to keep concerns clear:
 * <ul>
 *     <li>{@link PreparedStatementBinder} → binds parameters</li>
 *     <li>{@code StatementConfigurer} → configures execution behavior</li>
 * </ul>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * StatementConfigurer timeout = stmt -> stmt.setQueryTimeout(5);
 * StatementConfigurer fetch   = stmt -> stmt.setFetchSize(100);
 *
 * StatementConfigurer config = StatementConfigurer.combine(timeout, fetch);
 * }</pre>
 *
 * <p>
 * Configurers are typically applied <b>before</b> statement execution
 * and <b>after</b> parameter binding.
 *
 * @author jMouse
 */
@FunctionalInterface
public interface StatementConfigurer {

    /**
     * Applies configuration to the given JDBC {@link Statement}.
     *
     * @param statement JDBC statement to configure
     * @throws SQLException if the driver rejects a configuration option
     */
    void configure(Statement statement) throws SQLException;

    /**
     * No-op {@link StatementConfigurer} that performs no configuration.
     * <p>
     * Used as a default to avoid {@code null} checks.
     */
    StatementConfigurer NOOP = noop();

    /**
     * Returns a no-op {@link StatementConfigurer}.
     *
     * @return no-op configurer
     */
    static StatementConfigurer noop() {
        return new StatementConfigurer() {
            @Override
            public void configure(Statement statement) throws SQLException {
            }

            @Override
            public String toString() {
                return "NOOP";
            }
        };
    }

    /**
     * Creates a {@link StatementConfigurer} that sets the JDBC query timeout.
     * <p>
     * Internally delegates to {@link Statement#setQueryTimeout(int)}.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * StatementConfigurer cfg = StatementConfigurer.timeout(5);
     * }</pre>
     *
     * @param seconds timeout value in seconds
     * @return statement configurer applying the given timeout
     */
    static StatementConfigurer timeout(int seconds) {
        return statement -> statement.setQueryTimeout(seconds);
    }

    /**
     * Creates a {@link StatementConfigurer} that sets the JDBC fetch size.
     * <p>
     * Fetch size is a hint to the JDBC driver about how many rows should be
     * fetched from the database in one round-trip.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * StatementConfigurer cfg = StatementConfigurer.fetchSize(500);
     * }</pre>
     *
     * @param size fetch size hint
     * @return statement configurer applying the given fetch size
     */
    static StatementConfigurer fetchSize(int size) {
        return statement -> statement.setFetchSize(size);
    }

    /**
     * Combines two {@link StatementConfigurer} instances into one.
     * <p>
     * Configuration order is deterministic:
     * <ol>
     *     <li>first {@code a.configure(statement)}</li>
     *     <li>then {@code b.configure(statement)}</li>
     * </ol>
     *
     * <p>
     * {@link #NOOP} and {@code null} are treated as identity values.
     *
     * @param a first configurer
     * @param b second configurer
     * @return combined configurer
     */
    static StatementConfigurer combine(StatementConfigurer a, StatementConfigurer b) {
        if (a == null || a == NOOP) return (b != null) ? b : NOOP;
        if (b == null || b == NOOP) return a;
        return statement -> {
            a.configure(statement);
            b.configure(statement);
        };
    }

}
