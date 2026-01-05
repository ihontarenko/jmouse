package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link RowMetadata} implementation backed by JDBC {@link ResultSetMetaData}.
 * <p>
 * {@code ResultSetRowMetadata} captures column name → column index mappings
 * for a {@link ResultSet} and exposes them in an immutable form.
 *
 * <p>
 * Both {@link ResultSetMetaData#getColumnName(int)} and
 * {@link ResultSetMetaData#getColumnLabel(int)} are considered:
 * <ul>
 *     <li>column name is registered first</li>
 *     <li>column label is registered only if not already present</li>
 * </ul>
 * This mirrors common JDBC behavior where labels (aliases) should override
 * physical column names when present.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * RowMetadata metadata = ResultSetRowMetadata.of(resultSet);
 *
 * int idIndex = metadata.indexMap().get("id");
 * int nameIdx = metadata.indexMap().get("name");
 * }</pre>
 *
 * <p>
 * If metadata extraction fails for any reason, an empty metadata instance
 * is returned instead of propagating an exception.
 *
 * @author jMouse
 */
public final class ResultSetRowMetadata implements RowMetadata {

    /**
     * Immutable mapping of column name/label to JDBC column index (1-based).
     */
    private final Map<String, Integer> index;

    private ResultSetRowMetadata(Map<String, Integer> index) {
        this.index = index;
    }

    /**
     * Creates {@link RowMetadata} for the given {@link ResultSet}.
     *
     * @param resultSet JDBC result set
     * @return row metadata derived from {@link ResultSetMetaData}
     */
    public static RowMetadata of(ResultSet resultSet) {
        try {
            ResultSetMetaData    metaData = resultSet.getMetaData();
            int                  count    = metaData.getColumnCount();
            Map<String, Integer> mapping  = new HashMap<>(count * 2);

            for (int index = 1; index <= count; index++) {
                Optional<String> name  = Optional.ofNullable(metaData.getColumnName(index));
                Optional<String> label = Optional.ofNullable(metaData.getColumnLabel(index));
                int[]            cache = new int[]{index};
                name.ifPresent(n -> mapping.put(n, cache[0]));
                label.ifPresent(n -> mapping.putIfAbsent(n, cache[0]));
            }

            return new ResultSetRowMetadata(mapping);
        } catch (SQLException ignore) {
            // Fallback to empty metadata
        }

        return new ResultSetRowMetadata(Collections.emptyMap());
    }

    /**
     * Returns an immutable view of the column index mapping.
     *
     * @return map of column name/label to column index (1-based)
     */
    @Override
    public Map<String, Integer> indexMap() {
        return Map.copyOf(index);
    }

}
