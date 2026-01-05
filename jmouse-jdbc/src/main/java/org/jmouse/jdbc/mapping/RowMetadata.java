package org.jmouse.jdbc.mapping;

import org.jmouse.core.Contract;

import java.util.List;
import java.util.Map;

/**
 * Abstraction representing metadata of a single JDBC result row.
 * <p>
 * {@code RowMetadata} provides access to column index information derived
 * from a {@link java.sql.ResultSet}, allowing column lookup by name or label
 * without repeatedly querying JDBC metadata.
 *
 * <p>
 * Implementations are expected to:
 * <ul>
 *     <li>cache column metadata eagerly</li>
 *     <li>return stable, immutable views</li>
 *     <li>use <b>1-based</b> JDBC column indexes</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * RowMetadata meta = ResultSetRowMetadata.of(resultSet);
 *
 * int idIndex = meta.indexOf("id");
 * Object value = resultSet.getObject(idIndex);
 * }</pre>
 *
 * <p>
 * This abstraction is primarily used by generic mappers such as
 * {@code MapRowMapper} and low-level row access utilities.
 *
 * @author jMouse
 */
public interface RowMetadata {

    /**
     * Returns a cached mapping of column label/name to JDBC column index.
     * <p>
     * The returned map:
     * <ul>
     *     <li>must use 1-based column indexes</li>
     *     <li>must be stable for the lifetime of this metadata instance</li>
     *     <li>must not allocate a new map on each invocation</li>
     * </ul>
     *
     * @return column label/name → 1-based column index map
     */
    Map<String, Integer> indexMap();

    /**
     * Resolves the JDBC column index for the given column name or label.
     *
     * @param column column name or label
     * @return 1-based JDBC column index
     * @throws IllegalArgumentException if the column is not present
     */
    default int indexOf(String column) {
        Map<String, Integer> index = indexMap();
        Integer i = index.get(column);
        Contract.state(i != null, () -> new IllegalArgumentException("Unknown column '" + column + "'"));
        return i;
    }

    /**
     * Returns all known column names/labels.
     *
     * @return immutable list of column names
     */
    default List<String> names() {
        return List.copyOf(indexMap().keySet());
    }

    /**
     * Returns all known column indexes.
     *
     * @return immutable list of 1-based column indexes
     */
    default List<Integer> indexes() {
        return List.copyOf(indexMap().values());
    }

    /**
     * Returns the number of columns in this metadata.
     *
     * @return column count
     */
    default int count() {
        return indexMap().size();
    }

}
