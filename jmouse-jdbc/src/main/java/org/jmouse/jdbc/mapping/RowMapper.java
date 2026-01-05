package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Strategy interface for mapping a single row of a JDBC {@link ResultSet}
 * into a domain object.
 * <p>
 * {@code RowMapper} is designed for <b>row-by-row</b> transformation and is
 * typically used by higher-level extractors such as:
 * <ul>
 *     <li>{@code ListResultSetExtractor}</li>
 *     <li>{@code SingleResultSetExtractor}</li>
 *     <li>{@code StrictSingleResultSetExtractor}</li>
 * </ul>
 *
 * <h3>Row index semantics</h3>
 * <p>
 * The {@code rowIndex} parameter is zero-based and is maintained by the framework,
 * not by the JDBC driver. It can be useful for diagnostics, ordering, or
 * index-based logic.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * RowMapper<User> mapper = (rs, i) ->
 *     new User(rs.getLong("id"), rs.getString("name"));
 *
 * List<User> users = jdbc.query(
 *     "select id, name from users",
 *     mapper
 * );
 * }</pre>
 *
 * <p>
 * Note: {@link ResultSet} lifecycle is managed by the executor/template.
 * Implementations should not close the result set.
 *
 * @param <T> mapped element type
 *
 * @author jMouse
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current row of the given {@link ResultSet} to an object.
     *
     * @param resultSet JDBC result set positioned on the current row
     * @param rowIndex  zero-based row index
     * @return mapped object
     * @throws SQLException if JDBC access fails during mapping
     */
    T map(ResultSet resultSet, int rowIndex) throws SQLException;
}
