package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link ResultSetExtractor} that returns the <b>first</b> row of a result set,
 * or {@code null} if no rows are present.
 * <p>
 * {@code SingleResultSetExtractor} performs a lenient, non-strict extraction:
 * <ul>
 *     <li>0 rows → returns {@code null}</li>
 *     <li>1 row  → maps and returns the row</li>
 *     <li>more rows → ignores the rest</li>
 * </ul>
 *
 * <p>
 * This extractor is useful when:
 * <ul>
 *     <li>query semantics allow an optional single row</li>
 *     <li>you do not want to fail on empty results</li>
 *     <li>you are only interested in the first match</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * User user = jdbc.query(
 *     "select * from users where email = ?",
 *     ps -> ps.setString(1, "test@example.com"),
 *     new SingleResultSetExtractor<>(
 *         (rs, i) -> new User(rs.getLong("id"), rs.getString("name"))
 *     )
 * );
 * }</pre>
 *
 * <p>
 * For strict cardinality guarantees (exactly one row), prefer
 * {@link StrictSingleResultSetExtractor}.
 *
 * @param <T> mapped element type
 *
 * @author jMouse
 */
public final class SingleResultSetExtractor<T> implements ResultSetExtractor<T> {

    /**
     * Delegate mapper used to map the first row.
     */
    private final RowMapper<T> mapper;

    /**
     * Creates a new {@code SingleResultSetExtractor}.
     *
     * @param mapper row mapper for the first result row
     */
    public SingleResultSetExtractor(RowMapper<T> mapper) {
        this.mapper = mapper;
    }

    /**
     * Extracts the first row from the {@link ResultSet}, if present.
     *
     * @param resultSet JDBC result set positioned before the first row
     * @return mapped value of the first row, or {@code null} if no rows exist
     * @throws SQLException if JDBC access fails during extraction
     */
    @Override
    public T extract(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return mapper.map(resultSet, 1);
        }

        return null;
    }
}
