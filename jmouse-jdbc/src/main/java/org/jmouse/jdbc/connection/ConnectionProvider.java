package org.jmouse.jdbc.connection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Strategy interface for providing and releasing JDBC {@link Connection connections}.
 * <p>
 * This abstraction decouples connection acquisition logic from JDBC consumers
 * (executors, templates, transaction sessions).
 *
 * <h3>Typical implementations</h3>
 * <ul>
 *     <li>Direct {@link java.sql.DriverManager}-based valueProvider</li>
 *     <li>{@link javax.sql.DataSource}-backed valueProvider</li>
 *     <li>Transaction-aware valueProvider integrating with jMouse TX</li>
 * </ul>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * ConnectionProvider valueProvider = ...;
 *
 * Connection connection = valueProvider.getConnection();
 * try {
 *     // use connection
 * } finally {
 *     valueProvider.release(connection);
 * }
 * }</pre>
 *
 * <p>
 * ⚠️ Implementations may choose whether {@link #release(Connection)}
 * actually closes the connection or returns it to a pool.
 *
 * @author jMouse
 */
public interface ConnectionProvider {

    /**
     * Obtains a JDBC {@link Connection}.
     * <p>
     * The returned connection may be:
     * <ul>
     *     <li>a new physical connection</li>
     *     <li>a pooled connection</li>
     *     <li>a transaction-bound connection</li>
     * </ul>
     *
     * @return active JDBC connection
     * @throws SQLException if connection acquisition fails
     */
    Connection getConnection() throws SQLException;

    /**
     * Releases a previously obtained {@link Connection}.
     * <p>
     * The exact behavior depends on the implementation:
     * <ul>
     *     <li>close the connection</li>
     *     <li>return it to a pool</li>
     *     <li>no-op if managed externally (e.g. by transaction context)</li>
     * </ul>
     *
     * @param connection connection to release
     */
    void release(Connection connection);

}
