package org.jmouse.jdbc.mapping.bind;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.jdbc.core.exception.ResultSetAccessException;
import org.jmouse.jdbc.mapping.ResultSetRowMetadata;
import org.jmouse.jdbc.mapping.RowMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetAccessor extends AbstractAccessor {

    private final RowMetadata metadata;

    public ResultSetAccessor(Object source) {
        super(source);
        this.metadata = ResultSetRowMetadata.of((ResultSet) source);
    }

    @Override
    public ObjectAccessor get(String name) {
        return get(metadata.indexOf(name));
    }

    @Override
    public ObjectAccessor get(int index) {
        try {
            return wrap(asType(ResultSet.class).getObject(index));
        } catch (SQLException e) {
            throw new ResultSetAccessException("Unable to retrieve value from result-set.", e);
        }
    }

    @Override
    public void set(String name, Object value) {
        throw new UnsupportedOperationException("Result set is immutable and unable to be modified");
    }

    @Override
    public void set(int index, Object value) {
        throw new UnsupportedOperationException("Result set is immutable and unable to be modified");
    }
}
