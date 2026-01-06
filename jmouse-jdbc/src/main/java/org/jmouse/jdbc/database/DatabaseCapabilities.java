package org.jmouse.jdbc.database;

/**
 * Describes feature-level capabilities of a database platform.
 * <p>
 * {@code DatabaseCapabilities} is a compact, immutable descriptor that captures
 * which JDBC and SQL features are supported by a particular database vendor/version.
 * It is typically derived at startup time from {@link java.sql.DatabaseMetaData}
 * and/or vendor-specific knowledge.
 *
 * <p>
 * These flags are used by higher-level components (executors, templates, SQL builders)
 * to conditionally enable or disable certain execution strategies.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * if (platform.capabilities().supportsReturning()) {
 *     // use INSERT ... RETURNING
 * } else {
 *     // fallback to getGeneratedKeys()
 * }
 *
 * if (platform.capabilities().supportsOffsetFetch()) {
 *     sql = pagination.apply(sql, page);
 * }
 * }</pre>
 *
 * @param supportsBatch              whether JDBC batch updates are supported
 * @param supportsGeneratedKeys      whether {@link java.sql.Statement#getGeneratedKeys()}
 *                                   is supported
 * @param supportsSequences          whether database sequences are supported
 * @param supportsOffsetFetch        whether OFFSET / FETCH (or LIMIT/OFFSET equivalent)
 *                                   pagination is supported
 * @param supportsReturning          whether SQL {@code RETURNING} clauses are supported
 * @param supportsScrollableResults  whether scrollable {@link java.sql.ResultSet}s
 *                                   are supported
 *
 * @author jMouse
 */
public record DatabaseCapabilities(
        boolean supportsBatch,
        boolean supportsGeneratedKeys,
        boolean supportsSequences,
        boolean supportsOffsetFetch,
        boolean supportsReturning,
        boolean supportsScrollableResults
) {

    /**
     * Returns a conservative default capability set.
     * <p>
     * Defaults assume a reasonably modern JDBC driver with:
     * <ul>
     *     <li>batch updates enabled</li>
     *     <li>generated keys supported</li>
     *     <li>no native sequences</li>
     *     <li>no OFFSET/FETCH pagination</li>
     *     <li>no RETURNING clause</li>
     *     <li>no scrollable result sets</li>
     * </ul>
     *
     * @return default capabilities
     */
    public static DatabaseCapabilities defaults() {
        return new DatabaseCapabilities(true, true, false, false, false, false);
    }
}
