package org.jmouse.jdbc.mapping;

import java.sql.SQLException;

/**
 * {@code RowAccess} defines a minimal, uniform contract for reading values
 * from a JDBC result row.
 * <p>
 * The interface abstracts column access by:
 * <ul>
 *     <li><b>Index</b> — direct JDBC-style access (1-based)</li>
 *     <li><b>Name / label</b> — resolved via {@link #indexOf(String)}</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *     <li>Typed accessors return boxed types to allow {@code null} values</li>
 *     <li>Default methods delegate name-based access to index-based methods</li>
 *     <li>Implementations are free to cache column indices</li>
 * </ul>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * RowAccess row = ...;
 *
 * String  name   = row.getString("name");
 * Long    id     = row.getLong("id");
 * Boolean active = row.getBoolean("active");
 * }</pre>
 *
 * @author jMouse
 */
public interface RowAccess {

    /**
     * Resolves a column index by its name or label.
     *
     * @param column column name or alias
     * @return 1-based JDBC column index
     * @throws IllegalArgumentException if the column cannot be resolved
     */
    int indexOf(String column);

    /**
     * Reads a {@link String} value by column name.
     */
    default String getString(String column) throws SQLException {
        return getString(indexOf(column));
    }

    /**
     * Reads a {@link Long} value by column name.
     * <p>
     * Returns {@code null} if the underlying SQL value is {@code NULL}.
     */
    default Long getLong(String column) throws SQLException {
        return getLong(indexOf(column));
    }

    /**
     * Reads an {@link Integer} value by column name.
     * <p>
     * Returns {@code null} if the underlying SQL value is {@code NULL}.
     */
    default Integer getInteger(String column) throws SQLException {
        return getInteger(indexOf(column));
    }

    /**
     * Reads a {@link Boolean} value by column name.
     * <p>
     * Returns {@code null} if the underlying SQL value is {@code NULL}.
     */
    default Boolean getBoolean(String column) throws SQLException {
        return getBoolean(indexOf(column));
    }

    /**
     * Reads a raw {@link Object} value by column name.
     */
    default Object getObject(String column) throws SQLException {
        return getObject(indexOf(column));
    }

    /**
     * Reads a typed value by column name.
     *
     * @param column column name or alias
     * @param type   expected Java type
     * @param <T>    type parameter
     * @return value converted to the requested type
     */
    default <T> T getObject(String column, Class<T> type) throws SQLException {
        return getObject(indexOf(column), type);
    }

    /**
     * Reads a {@link String} value by column index.
     */
    String getString(int index) throws SQLException;

    /**
     * Reads a {@link Long} value by column index.
     * <p>
     * Returns {@code null} if the underlying SQL value is {@code NULL}.
     */
    Long getLong(int index) throws SQLException;

    /**
     * Reads an {@link Integer} value by column index.
     * <p>
     * Returns {@code null} if the underlying SQL value is {@code NULL}.
     */
    Integer getInteger(int index) throws SQLException;

    /**
     * Reads a {@link Boolean} value by column index.
     * <p>
     * Returns {@code null} if the underlying SQL value is {@code NULL}.
     */
    Boolean getBoolean(int index) throws SQLException;

    /**
     * Reads a raw {@link Object} value by column index.
     */
    Object getObject(int index) throws SQLException;

    /**
     * Reads a typed value by column index.
     *
     * @param index column index (1-based)
     * @param type  expected Java type
     * @param <T>   type parameter
     * @return value converted to the requested type
     */
    <T> T getObject(int index, Class<T> type) throws SQLException;

}
