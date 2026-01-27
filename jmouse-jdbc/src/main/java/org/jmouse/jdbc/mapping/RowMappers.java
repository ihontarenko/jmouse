package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;

/**
 * Factory class providing common {@link RowMapper} implementations for
 * single-column scalar results.
 * <p>
 * {@code RowMappers} offers convenience methods for mapping a specific
 * JDBC column (by index) to a boxed Java type, handling SQL {@code NULL}
 * values consistently.
 *
 * <h3>Design notes</h3>
 * <ul>
 *     <li>All column indexes are <b>1-based</b> (JDBC semantics)</li>
 *     <li>SQL {@code NULL} values are mapped to Java {@code null}</li>
 *     <li>Built on top of {@link ColumnRowMapper}</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * List<Long> ids = jdbc.query(
 *     "select id from users",
 *     RowMappers.longColumn(1)
 * );
 *
 * String name = jdbc.queryOne(
 *     "select name from users where id = ?",
 *     ps -> ps.setLong(1, 10L),
 *     RowMappers.stringColumn(1)
 * );
 * }</pre>
 *
 * <p>
 * This class is not instantiable.
 *
 * @author jMouse
 */
public final class RowMappers {

    private RowMappers() {}

    /**
     * Creates a {@link RowMapper} that reads a {@link String} value
     * from the given column index.
     *
     * @param index JDBC column index (1-based)
     * @return string column row mapperProvider
     */
    public static RowMapper<String> stringColumn(int index) {
        return new ColumnRowMapper<>(index, ResultSet::getString);
    }

    /**
     * Creates a {@link RowMapper} that reads a {@link Long} value
     * from the given column index.
     * <p>
     * SQL {@code NULL} values are mapped to {@code null}.
     *
     * @param index JDBC column index (1-based)
     * @return long column row mapperProvider
     */
    public static RowMapper<Long> longColumn(int index) {
        return new ColumnRowMapper<>(index, (resultSet, i)
                -> resultSet.wasNull() ? null : resultSet.getLong(i));
    }

    /**
     * Creates a {@link RowMapper} that reads an {@link Integer} value
     * from the given column index.
     * <p>
     * SQL {@code NULL} values are mapped to {@code null}.
     *
     * @param index JDBC column index (1-based)
     * @return integer column row mapperProvider
     */
    public static RowMapper<Integer> integerColumn(int index) {
        return new ColumnRowMapper<>(index, (resultSet, i)
                -> resultSet.wasNull() ? null : resultSet.getInt(i));
    }

    /**
     * Creates a {@link RowMapper} that reads a {@link Boolean} value
     * from the given column index.
     * <p>
     * SQL {@code NULL} values are mapped to {@code null}.
     *
     * @param index JDBC column index (1-based)
     * @return boolean column row mapperProvider
     */
    public static RowMapper<Boolean> booleanColumn(int index) {
        return new ColumnRowMapper<>(index, (resultSet, i)
                -> resultSet.wasNull() ? null : resultSet.getBoolean(i));
    }

}
