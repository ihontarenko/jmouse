package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Strategy interface for extracting an arbitrary result from a JDBC {@link ResultSet}.
 * <p>
 * {@code ResultSetExtractor} is the lowest-level mapping hook in the jMouse JDBC layer.
 * It receives a {@link ResultSet} (typically positioned <b>before</b> the first row)
 * and may iterate it in any way required to produce a result.
 *
 * <h3>When to use</h3>
 * <ul>
 *     <li>Custom multi-row aggregation (grouping, projections, joins)</li>
 *     <li>Streaming-style processing (driver permitting)</li>
 *     <li>Non-list results (maps, scalars, domain aggregates)</li>
 *     <li>Advanced cursor manipulation</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * ResultSetExtractor<List<User>> extractor = rs -> {
 *     List<User> users = new ArrayList<>();
 *     while (rs.next()) {
 *         users.add(new User(rs.getLong("id"), rs.getString("name")));
 *     }
 *     return users;
 * };
 *
 * List<User> users = executor.execute(
 *     "select id, name from users",
 *     StatementCallback.QUERY,
 *     extractor
 * );
 * }</pre>
 *
 * <p>
 * Note: {@link ResultSet} lifecycle is owned by the executor/template. Implementations
 * should not close the result set.
 *
 * @param <T> extracted result type
 *
 * @author jMouse
 */
@FunctionalInterface
public interface ResultSetExtractor<T> {

    /**
     * Extracts a result object from the given {@link ResultSet}.
     *
     * @param resultSet JDBC result set (cursor position is executor-defined)
     * @return extracted result
     * @throws SQLException if JDBC access fails during extraction
     */
    T extract(ResultSet resultSet) throws SQLException;
}
