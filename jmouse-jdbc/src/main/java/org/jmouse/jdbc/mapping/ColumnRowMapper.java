package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link RowMapper} decorator that delegates mapping to another mapper
 * while forcing a specific column index.
 * <p>
 * {@code ColumnRowMapper} adapts an existing {@link RowMapper} so that it
 * always reads from a fixed column index instead of relying on the
 * framework-provided {@code rowIndex}.
 *
 * <p>
 * This is useful when:
 * <ul>
 *     <li>mapping scalar results (single-column queries)</li>
 *     <li>reusing an existing mapper that expects a column index</li>
 *     <li>bridging APIs where row index and column index semantics differ</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * RowMapper<Long> idMapper =
 *     new ColumnRowMapper<>(1, (rs, col) -> rs.getLong(col));
 *
 * List<Long> ids = jdbc.query(
 *     "select id from users",
 *     idMapper
 * );
 * }</pre>
 *
 * <p>
 * Note: The {@code rowIndex} provided by the framework is intentionally ignored
 * and replaced with the configured {@code columnIndex}.
 *
 * @param <T> mapped element type
 *
 * @author jMouse
 */
public final class ColumnRowMapper<T> implements RowMapper<T> {

    /**
     * Fixed column index to be used for mapping.
     * <p>
     * JDBC column indexes are 1-based.
     */
    private final int columnIndex;

    /**
     * Delegate row mapper.
     */
    private final RowMapper<T> rowMapper;

    /**
     * Creates a new {@code ColumnRowMapper}.
     *
     * @param columnIndex JDBC column index (1-based)
     * @param rowMapper   delegate mapper that will receive the column index
     */
    public ColumnRowMapper(int columnIndex, RowMapper<T> rowMapper) {
        this.columnIndex = columnIndex;
        this.rowMapper = rowMapper;
    }

    /**
     * Maps the current row using the configured column index.
     *
     * @param resultSet JDBC result set positioned on the current row
     * @param rowIndex  original row index (ignored)
     * @return mapped value
     * @throws SQLException if JDBC access fails
     */
    @Override
    public T map(ResultSet resultSet, int rowIndex) throws SQLException {
        return rowMapper.map(resultSet, columnIndex);
    }

}
