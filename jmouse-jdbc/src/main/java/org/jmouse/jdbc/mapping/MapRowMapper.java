package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link RowMapper} implementation that maps a single {@link ResultSet} row
 * into a {@link Map} keyed by column names.
 * <p>
 * Each column present in the current row is exposed as an entry in the resulting
 * map, where:
 * <ul>
 *     <li>the key is the column name as reported by {@link RowMetadata}</li>
 *     <li>the value is obtained via {@link ResultSet#getObject(String)}</li>
 * </ul>
 *
 * <p>
 * This mapper is particularly useful for:
 * <ul>
 *     <li>ad-hoc queries</li>
 *     <li>dynamic projections</li>
 *     <li>debugging and inspection</li>
 *     <li>cases where no fixed domain model exists</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * List<Map<String, Object>> rows = jdbc.query(
 *     "select * from users",
 *     new MapRowMapper()
 * );
 *
 * Object name = rows.get(0).get("name");
 * }</pre>
 *
 * <p>
 * Column name resolution is delegated to {@link ResultSetRowMetadata},
 * which typically exposes column labels or names depending on the driver.
 *
 * @author jMouse
 */
public class MapRowMapper implements RowMapper<Map<String, Object>> {

    /**
     * Maps the current {@link ResultSet} row into a {@link Map}.
     *
     * @param resultSet JDBC result set positioned on the current row
     * @param rowIndex  zero-based row index (not used)
     * @return map of column name to column value
     * @throws SQLException if JDBC access fails
     */
    @Override
    public Map<String, Object> map(ResultSet resultSet, int rowIndex) throws SQLException {
        RowMetadata         metadata = ResultSetRowMetadata.of(resultSet);
        Map<String, Object> map      = new HashMap<>();

        for (String name : metadata.names()) {
            map.put(name, resultSet.getObject(name));
        }

        return map;
    }

}
