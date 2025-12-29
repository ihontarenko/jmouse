package org.jmouse.jdbc.connection.datasource;

/**
 * Thread-bound holder for the current {@link DataSourceKey}.
 * <p>
 * {@code DataSourceKeyHolder} provides a lightweight context mechanism
 * for routing JDBC operations to different {@link javax.sql.DataSource}
 * instances based on the current thread.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * DataSourceKeyHolder.use("read-replica");
 * try {
 *     jdbc.query("select * from users", mapper);
 * } finally {
 *     DataSourceKeyHolder.clear();
 * }
 * }</pre>
 *
 * <h3>Scope and lifecycle</h3>
 * <ul>
 *     <li>Backed by {@link ThreadLocal}</li>
 *     <li>Must be explicitly cleared to avoid context leakage</li>
 *     <li>Intended to be used together with routing {@code ConnectionProvider}</li>
 * </ul>
 *
 * <p>
 * ⚠️ This holder does not validate keys and does not enforce nesting rules.
 * Higher-level infrastructure is responsible for correct usage.
 *
 * @author jMouse
 */
public class DataSourceKeyHolder {

    /**
     * Thread-local storage for the current data source key.
     */
    private static final ThreadLocal<String> KEY = new ThreadLocal<>();

    private DataSourceKeyHolder() {}

    /**
     * Binds the given data source key to the current thread.
     *
     * @param key logical data source key
     */
    public static void use(String key) { KEY.set(key); }

    /**
     * Returns the currently bound data source key.
     *
     * @return current key or {@code null} if none is bound
     */
    public static String current() { return KEY.get(); }

    /**
     * Clears the currently bound data source key from the thread.
     */
    public static void clear() { KEY.remove(); }

}
