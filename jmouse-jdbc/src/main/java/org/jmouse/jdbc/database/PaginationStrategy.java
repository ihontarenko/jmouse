package org.jmouse.jdbc.database;

import org.jmouse.jdbc.query.OffsetLimit;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Strategy interface for applying database-specific pagination.
 * <p>
 * {@code PaginationStrategy} encapsulates how {@link OffsetLimit} pagination
 * is rendered and bound for a particular SQL dialect (e.g. LIMIT/OFFSET,
 * ROWNUM, FETCH FIRST, window functions).
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Transform the original SQL to include pagination clauses</li>
 *     <li>Provide a binder for pagination parameters</li>
 *     <li>Declare how many JDBC parameters are introduced</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * String pagedSql = strategy.apply(
 *     "select * from users order by id",
 *     OffsetLimit.of(20, 10)
 * );
 *
 * PaginationStrategy.PaginationBind bind = strategy.bind(OffsetLimit.of(20, 10));
 * bind.bind(statement, baseParameterCount + 1);
 * }</pre>
 *
 * <p>
 * The separation between SQL rewriting ({@link #apply(String, OffsetLimit)})
 * and parameter binding ({@link #bind(OffsetLimit)}) allows safe composition
 * with existing binders.
 *
 * @author jMouse
 */
public interface PaginationStrategy {

    /**
     * Applies pagination to the given SQL statement.
     *
     * @param sql  original SQL query
     * @param page pagination parameters
     * @return SQL string with pagination applied
     */
    String apply(String sql, OffsetLimit page);

    /**
     * Returns a binder responsible for binding pagination parameters.
     *
     * @param page pagination parameters
     * @return pagination parameter binder
     */
    PaginationBind bind(OffsetLimit page);

    /**
     * Contract for binding pagination-related parameters.
     * <p>
     * Implementations must bind parameters starting from the provided
     * JDBC index and report how many parameters they consume.
     */
    interface PaginationBind {

        /**
         * Binds pagination parameters into the prepared statement.
         *
         * @param statement prepared statement
         * @param startIndex 1-based JDBC index to start binding from
         * @throws SQLException if JDBC access fails
         */
        void bind(PreparedStatement statement, int startIndex) throws SQLException;

        /**
         * Returns the number of JDBC parameters introduced by pagination.
         *
         * @return parameter count
         */
        int countOfParameters();
    }
}
