package org.jmouse.query.sql.mapping;

/**
 * A table one hop away, and the two columns that get there.
 *
 * <p>{@code new JoinedTable("statuses", "status_id", "id")} — <em>our</em> {@code status_id} points at
 * <em>their</em> {@code id}. An attribute then names a column of it: {@code statuses.category}.</p>
 *
 * <p>⚠️ Deliberately three names and no join type, no condition and no direction. It is always a
 * {@code LEFT JOIN} on equality, because that is the only shape a declared source needs: a normalised
 * value that may or may not be there. Anything richer is a query, and a query is what the language is
 * for — a mapping that could express one would be a second place to write queries, unreadable and
 * unchecked.</p>
 *
 * @param table         the table one hop away — {@code statuses}
 * @param localColumn   the column on our row pointing at it — {@code status_id}
 * @param foreignColumn the column on its row being pointed at — {@code id}
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record JoinedTable(String table, String localColumn, String foreignColumn) {
}
