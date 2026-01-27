package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ResultSetExtractor} implementation that maps all rows of a
 * {@link ResultSet} into a {@link List}.
 * <p>
 * {@code ListResultSetExtractor} iterates over the entire result set and
 * delegates per-row mapping to a provided {@link RowMapper}.
 *
 * <p>
 * This extractor is the canonical implementation for queries expected to
 * return zero or more rows.
 *
 * <h3>Row index semantics</h3>
 * <p>
 * The {@code rowIndex} passed to the {@link RowMapper} is:
 * <ul>
 *     <li>1-based</li>
 *     <li>incremented sequentially for each processed row</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * List<User> users = jdbc.query(
 *     "select id, name from users",
 *     new ListResultSetExtractor<>(
 *         (rs, i) -> new User(rs.getLong("id"), rs.getString("name"))
 *     )
 * );
 * }</pre>
 *
 * <p>
 * Note: The {@link ResultSet} lifecycle is managed by the executor/template.
 * Implementations must not close the result set.
 *
 * @param <T> mapped element type
 *
 * @author jMouse
 */
public final class ListResultSetExtractor<T> implements ResultSetExtractor<List<T>> {

    /**
     * Delegate row mapperProvider used for each result row.
     */
    private final RowMapper<T> mapper;

    /**
     * Creates a new {@code ListResultSetExtractor}.
     *
     * @param mapper row mapperProvider used to convert each {@link ResultSet} row
     */
    public ListResultSetExtractor(RowMapper<T> mapper) {
        this.mapper = mapper;
    }

    /**
     * Extracts all rows from the given {@link ResultSet} into a {@link List}.
     *
     * @param resultSet JDBC result set positioned before the first row
     * @return list of mapped elements (possibly empty)
     * @throws SQLException if JDBC access fails during iteration or mapping
     */
    @Override
    public List<T> extract(ResultSet resultSet) throws SQLException {
        List<T> collection = new ArrayList<>();
        int     rowIndex   = 1;

        while (resultSet.next()) {
            collection.add(mapper.map(resultSet, rowIndex++));
        }

        return collection;
    }
}
