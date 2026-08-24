package org.jmouse.query.sql.mapping;

/**
 * A table holding many rows per row — labels, tags, watchers.
 *
 * <p>{@code new CollectionTable("issue_labels", "issue_id", "label")} — every row of
 * {@code issue_labels} whose {@code issue_id} is ours holds one {@code label}.</p>
 *
 * <p>⚠️ Three names and <strong>no key column</strong>, which is the whole difference from a
 * {@link BagTable}: a bag row says <em>which</em> attribute it is, a collection row is simply one of
 * them. That is why a bag has a value and a collection only has a question.</p>
 *
 * @param table       where the items live — {@code issue_labels}
 * @param foreignKey  the column pointing back at the row they belong to — {@code issue_id}
 * @param valueColumn the column holding one item — {@code label}
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record CollectionTable(String table, String foreignKey, String valueColumn) {
}
