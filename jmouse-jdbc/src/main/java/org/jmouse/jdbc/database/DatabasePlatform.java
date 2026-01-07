package org.jmouse.jdbc.database;

import org.jmouse.jdbc.query.OffsetLimit;

/**
 * Complete runtime database profile.
 * <p>
 * {@code DatabasePlatform} represents a fully resolved database descriptor
 * used at runtime by the JDBC layer. It centralizes vendor identification,
 * versioning, capabilities, and SQL-related strategies behind a single contract.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Expose database identity and version information</li>
 *     <li>Provide SQL dialect strategies (pagination, quoting, templates)</li>
 *     <li>Describe supported capabilities and feature flags</li>
 *     <li>Optionally rewrite or normalize SQL before execution</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * DatabasePlatform platform = registry.resolve(connection);
 *
 * String sql = platform.pagination()
 *     .apply("select * from users order by id", OffsetLimit.of(0, 10));
 *
 * if (platform.capabilities().supportsReturning()) {
 *     // use RETURNING clause
 * }
 * }</pre>
 *
 * <p>
 * Implementations are usually immutable and cached per {@link javax.sql.DataSource}
 * or {@link java.sql.Connection}.
 *
 * @author jMouse
 */
public interface DatabasePlatform {

    /**
     * Returns the pagination strategy for this database.
     *
     * @return pagination strategy
     */
    PaginationStrategy pagination();

    /**
     * Returns the database identifier (vendor/product).
     *
     * @return database id
     */
    DatabaseId id();

    /**
     * Returns the database version descriptor.
     *
     * @return database version
     */
    DatabaseVersion version();

    /**
     * Returns the set of supported database capabilities.
     *
     * @return database capabilities
     */
    DatabaseCapabilities capabilities();

    /**
     * Returns the SQL quoting rules for identifiers.
     *
     * @return SQL quoting strategy
     */
    SQLQuoting quoting();

    /**
     * Returns database-specific SQL templates and builders.
     *
     * @return SQL templates
     */
    SQLTemplates sql();

    /**
     * Optional hook point for vendor-specific SQL normalization or rewrite.
     * <p>
     * This hook is invoked before SQL execution and may:
     * <ul>
     *     <li>rewrite vendor-specific syntax</li>
     *     <li>apply compatibility transformations</li>
     *     <li>normalize identifiers or functions</li>
     * </ul>
     *
     * @return rewrite hook (defaults to no-op)
     */
    default RewriteHook rewriteHook() {
        return RewriteHook.noop();
    }

    default String applyPagination(String sql, OffsetLimit page) {
        return pagination().apply(sql, page);
    }

    /**
     * Returns a human-readable label for logging and diagnostics.
     * <p>
     * The default format is:
     * <pre>{@code
     * vendor:product major.minor
     * }</pre>
     *
     * @return display name
     */
    default String displayName() {
        return "%s:%s %s".formatted(id().vendor(), id().product(), version());
    }
}
