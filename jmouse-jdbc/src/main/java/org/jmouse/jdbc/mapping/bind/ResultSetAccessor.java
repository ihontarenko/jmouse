package org.jmouse.jdbc.mapping.bind;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.jdbc.exception.ResultSetAccessException;
import org.jmouse.jdbc.mapping.ResultSetRowMetadata;
import org.jmouse.jdbc.mapping.RowMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link ObjectAccessor} implementation over a JDBC {@link ResultSet}.
 * <p>
 * {@code ResultSetAccessor} adapts a {@link ResultSet} to the jMouse binding API,
 * enabling column-based reads via:
 * <ul>
 *     <li>column label/name: {@link #get(String)}</li>
 *     <li>column index (1-based): {@link #get(int)}</li>
 * </ul>
 *
 * <p>
 * Column lookup is backed by {@link RowMetadata} resolved once during construction
 * using {@link ResultSetRowMetadata}.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * ResultSetAccessor accessor = new ResultSetAccessor(rs);
 *
 * Object id = accessor.get("id").getValue();
 * Object name = accessor.get("name").getValue();
 * }</pre>
 *
 * <p>
 * This accessor is read-only: all {@code set(...)} methods throw
 * {@link UnsupportedOperationException}.
 *
 * @author jMouse
 */
public class ResultSetAccessor extends AbstractAccessor {

    /**
     * Cached metadata for resolving column name/label to 1-based JDBC index.
     */
    private final RowMetadata metadata;

    /**
     * Creates a new accessor for the given {@link ResultSet}.
     *
     * @param source result set instance
     */
    public ResultSetAccessor(Object source) {
        super(source);
        this.metadata = ResultSetRowMetadata.of((ResultSet) source);
    }

    /**
     * Resolves the given column name/label and returns an accessor for its value.
     *
     * @param name column label or name
     * @return accessor over the column value
     * @throws IllegalArgumentException if the column is unknown
     */
    @Override
    public ObjectAccessor get(String name) {
        return get(metadata.indexOf(name));
    }

    /**
     * Returns an accessor for the value at the given JDBC column index.
     *
     * @param index 1-based JDBC column index
     * @return accessor over the column value
     * @throws ResultSetAccessException if the value cannot be read from the result set
     */
    @Override
    public ObjectAccessor get(int index) {
        try {
            return wrap(asType(ResultSet.class).getObject(index));
        } catch (SQLException e) {
            throw new ResultSetAccessException("Unable to retrieve value from result-set.", e);
        }
    }

    /**
     * Not supported: a {@link ResultSet} is treated as immutable input.
     *
     * @param name  column name/label
     * @param value ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void set(String name, Object value) {
        throw new UnsupportedOperationException("Result set is immutable and unable to be modified");
    }

    /**
     * Not supported: a {@link ResultSet} is treated as immutable input.
     *
     * @param index column index
     * @param value ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void set(int index, Object value) {
        throw new UnsupportedOperationException("Result set is immutable and unable to be modified");
    }
}
