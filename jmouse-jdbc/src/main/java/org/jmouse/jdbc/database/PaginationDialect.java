package org.jmouse.jdbc.database;

/**
 * Dialect-aware SQL pagination strategy.
 */
public interface PaginationDialect {

    /**
     * Applies pagination to a SQL query.
     *
     * @param sql base query (without trailing semicolon)
     * @param offset row offset (>= 0)
     * @param limit max rows (> 0)
     * @return paginated SQL
     */
    String apply(String sql, long offset, int limit);

}