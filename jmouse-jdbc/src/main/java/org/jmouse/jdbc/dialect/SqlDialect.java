package org.jmouse.jdbc.dialect;

/**
 * Database-specific SQL dialect.
 *
 * <p>Encapsulates vendor differences (pagination, sequences, hints, quoting, etc.)
 * without leaking them into core JDBC execution.</p>
 */
public interface SqlDialect {

    /**
     * @return dialect id (e.g. "postgres", "mysql", "h2")
     */
    String id();

    /**
     * Apply vendor-specific LIMIT/OFFSET (or equivalent).
     */
    String limitOffset(String sql, int offset, int limit);

    /**
     * Build SQL for fetching next sequence value.
     */
    String sequenceNextValue(String sequence);

    /**
     * Quote an identifier (table/column) if needed.
     */
    default String quote(String identifier) {
        return identifier;
    }

    /**
     * @return {@code true} if dialect supports SQL standard OFFSET ... FETCH
     */
    default boolean supportsOffsetFetch() {
        return false;
    }
}
