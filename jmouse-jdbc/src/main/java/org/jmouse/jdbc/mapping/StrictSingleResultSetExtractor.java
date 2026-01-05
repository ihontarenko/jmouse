package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jmouse.jdbc.exception.EmptyResultException;
import org.jmouse.jdbc.exception.NonUniqueResultException;

/**
 * {@link ResultSetExtractor} that enforces <b>exactly one</b> result row.
 * <p>
 * {@code StrictSingleResultSetExtractor} validates result cardinality and fails fast
 * if the query returns:
 * <ul>
 *     <li>no rows → {@link EmptyResultException}</li>
 *     <li>more than one row → {@link NonUniqueResultException}</li>
 * </ul>
 *
 * <p>
 * The successfully mapped value is wrapped into {@link Optional} to preserve
 * null-safety semantics of the underlying {@link RowMapper}.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * Optional<User> user = jdbc.querySingle(
 *     "select * from users where id = ?",
 *     ps -> ps.setLong(1, 10L),
 *     new StrictSingleResultSetExtractor<>(
 *         (rs, i) -> new User(rs.getLong("id"), rs.getString("name")),
 *         "select * from users where id = ?"
 *     )
 * );
 * }</pre>
 *
 * <p>
 * This extractor is commonly used by higher-level APIs such as
 * {@code SimpleOperations.querySingle(...)} where strict cardinality
 * guarantees are required.
 *
 * @param <T> mapped element type
 *
 * @author jMouse
 */
public final class StrictSingleResultSetExtractor<T> implements ResultSetExtractor<Optional<T>> {

    /**
     * Delegate mapper used to map the single expected row.
     */
    private final RowMapper<T> mapper;

    /**
     * SQL string used for exception messages.
     */
    private final String sqlForMessage;

    /**
     * Creates a new {@code StrictSingleResultSetExtractor}.
     *
     * @param mapper         row mapper for the single result
     * @param sqlForMessage  SQL string included in exception messages
     */
    public StrictSingleResultSetExtractor(RowMapper<T> mapper, String sqlForMessage) {
        this.mapper = mapper;
        this.sqlForMessage = sqlForMessage;
    }

    /**
     * Extracts exactly one row from the {@link ResultSet}.
     *
     * @param resultSet JDBC result set positioned before the first row
     * @return optional containing the mapped value
     * @throws EmptyResultException     if no rows are returned
     * @throws NonUniqueResultException if more than one row is returned
     * @throws SQLException             if JDBC access fails
     */
    @Override
    public Optional<T> extract(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            throw new EmptyResultException("Expected 1 row but got 0 for SQL: " + sqlForMessage);
        }

        T value = mapper.map(resultSet, 1);

        if (resultSet.next()) {
            int count = 2;
            while (resultSet.next()) {
                count++;
            }
            throw new NonUniqueResultException(
                    count,
                    "Expected 1 row but got " + count + " for SQL: " + sqlForMessage
            );
        }

        return Optional.ofNullable(value);
    }
}
