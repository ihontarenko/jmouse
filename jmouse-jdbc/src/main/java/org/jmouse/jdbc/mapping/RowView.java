package org.jmouse.jdbc.mapping;

import org.jmouse.core.Contract;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code RowView} is a lightweight, read-only wrapper over a single {@link ResultSet} row.
 * <p>
 * It provides:
 * <ul>
 *     <li>Column access by <b>index</b> and <b>name/label</b></li>
 *     <li>Null-safe accessors for common JDBC types</li>
 *     <li>Cached column index lookup for fast repeated access</li>
 * </ul>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * ResultSet rs = statement.executeQuery();
 * if (rs.next()) {
 *     RowView row = RowView.of(rs);
 *
 *     String  name = row.getString("name");
 *     Long    id   = row.getLong(row.indexOf("id"));
 *     Boolean flag = row.getBoolean(row.indexOf("active"));
 * }
 * }</pre>
 *
 * <p>
 * ⚠️ {@code RowView} does not move the cursor. The caller is responsible
 * for calling {@link ResultSet#next()}.
 *
 * @author jMouse
 */
public final class RowView implements RowAccess {

    /** Underlying JDBC result set positioned at the current row */
    private final ResultSet resultSet;

    /** Total number of columns in the result set */
    private final int count;

    /**
     * Column name/label to column index mapping.
     * <p>
     * Both {@link ResultSetMetaData#getColumnName(int)} and
     * {@link ResultSetMetaData#getColumnLabel(int)} are supported.
     */
    private final Map<String, Integer> index;

    private RowView(ResultSet resultSet, int count, Map<String, Integer> index) {
        this.resultSet = resultSet;
        this.count = count;
        this.index = index;
    }

    /**
     * Creates a {@link RowView} for the current row of the given {@link ResultSet}.
     * <p>
     * Column indices are resolved once using {@link ResultSetMetaData}
     * and cached for fast access.
     *
     * @param resultSet active {@link ResultSet} positioned on a valid row
     * @return a new {@code RowView} instance
     * @throws SQLException if metadata access fails
     */
    public static RowView of(ResultSet resultSet) throws SQLException {
        ResultSetMetaData    metadata = resultSet.getMetaData();
        int                  count    = metadata.getColumnCount();
        Map<String, Integer> mapping  = new HashMap<>(count * 2);

        for (int index = 1; index <= count; index++) {
            Optional<String> name  = Optional.ofNullable(metadata.getColumnName(index));
            Optional<String> label = Optional.ofNullable(metadata.getColumnLabel(index));
            int[]            cache = new int[]{index};

            name.ifPresent(n -> mapping.put(n, cache[0]));
            label.ifPresent(n -> mapping.putIfAbsent(n, cache[0]));
        }

        return new RowView(resultSet, count, mapping);
    }

    /**
     * Resolves a column index by its name or label.
     *
     * @param column column name or alias
     * @return 1-based JDBC column index
     * @throws IllegalArgumentException if the column is unknown
     */
    @Override
    public int indexOf(String column) {
        Contract.state(
                index.containsKey(column),
                () -> new IllegalArgumentException("Unknown column '" + column + "'")
        );
        return index.get(column);
    }

    /**
     * Reads a {@link String} value from the given column index.
     */
    @Override
    public String getString(int index) throws SQLException {
        return resultSet.getString(index);
    }

    /**
     * Reads a {@link Long} value from the given column index.
     * <p>
     * Returns {@code null} if the underlying SQL value is {@code NULL}.
     */
    @Override
    public Long getLong(int index) throws SQLException {
        long value = resultSet.getLong(index);
        return resultSet.wasNull() ? null : value;
    }

    /**
     * Reads an {@link Integer} value from the given column index.
     * <p>
     * Returns {@code null} if the underlying SQL value is {@code NULL}.
     */
    @Override
    public Integer getInteger(int index) throws SQLException {
        int value = resultSet.getInt(index);
        return resultSet.wasNull() ? null : value;
    }

    /**
     * Reads a {@link Boolean} value from the given column index.
     * <p>
     * Returns {@code null} if the underlying SQL value is {@code NULL}.
     */
    @Override
    public Boolean getBoolean(int index) throws SQLException {
        boolean value = resultSet.getBoolean(index);
        return resultSet.wasNull() ? null : value;
    }

    /**
     * Reads a raw {@link Object} value from the given column index.
     */
    @Override
    public Object getObject(int index) throws SQLException {
        return resultSet.getObject(index);
    }

    /**
     * Reads a typed value from the given column index.
     *
     * @param index column index (1-based)
     * @param type  expected Java type
     * @param <T>   type parameter
     * @return value converted to the requested type
     * @throws SQLException if JDBC access fails or conversion is not possible
     */
    @Override
    public <T> T getObject(int index, Class<T> type) throws SQLException {
        return resultSet.getObject(index, type);
    }

    /**
     * Returns the total number of columns in the underlying {@link java.sql.ResultSet}.
     *
     * @return column count
     */
    public int getColumnCount() {
        return count;
    }

    /**
     * Returns an immutable list of all resolved column names and labels.
     * <p>
     * The list may contain both physical column names and aliases
     * (as defined by {@code AS ...} in SQL).
     *
     * @return immutable list of column names
     */
    public List<String> getNames() {
        return List.copyOf(index.keySet());
    }

    /**
     * Returns an immutable list of all resolved column indexes.
     * <p>
     * Indexes are JDBC-style and therefore <b>1-based</b>.
     *
     * @return immutable list of column indexes
     */
    public List<Integer> getIndexes() {
        return List.copyOf(index.values());
    }


}
