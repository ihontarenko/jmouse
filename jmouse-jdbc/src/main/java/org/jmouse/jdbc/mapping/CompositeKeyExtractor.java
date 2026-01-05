package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link KeyExtractor} implementation for composite (multi-column) generated keys.
 * <p>
 * {@code CompositeKeyExtractor} reads the first row of the generated-keys
 * {@link ResultSet} and returns a {@link Map} where:
 * <ul>
 *     <li>keys are column names/labels</li>
 *     <li>values are corresponding generated key values</li>
 * </ul>
 *
 * <p>
 * This extractor is useful for:
 * <ul>
 *     <li>tables with composite primary keys</li>
 *     <li>databases returning multiple generated columns</li>
 *     <li>generic insert logic where key structure is not known upfront</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Map<String, Object> keys = jdbc.update(
 *     "insert into orders(customer_id, created_at) values(?, ?)",
 *     ps -> {
 *         ps.setLong(1, 10L);
 *         ps.setTimestamp(2, Timestamp.from(Instant.now()));
 *     },
 *     new CompositeKeyExtractor()
 * );
 *
 * Object orderId = keys.get("order_id");
 * }</pre>
 *
 * <p>
 * If the generated-keys result set is empty, an empty map is returned.
 * The insertion order of columns is preserved.
 *
 * @author jMouse
 */
public final class CompositeKeyExtractor implements KeyExtractor<Map<String, Object>> {

    /**
     * Extracts a composite key from the generated-keys {@link ResultSet}.
     *
     * @param keys result set containing generated keys
     * @return map of column name/label to generated key value, or empty map if none
     * @throws SQLException if JDBC access fails during extraction
     */
    @Override
    public Map<String, Object> extract(ResultSet keys) throws SQLException {
        if (!keys.next()) {
            return Map.of();
        }

        Map<String, Object> result   = new LinkedHashMap<>();
        RowMetadata         metadata = ResultSetRowMetadata.of(keys);

        for (String name : metadata.names()) {
            result.put(name, keys.getObject(metadata.indexOf(name)));
        }

        return result;
    }
}
