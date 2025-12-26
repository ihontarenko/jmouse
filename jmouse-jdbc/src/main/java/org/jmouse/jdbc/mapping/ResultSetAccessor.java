package org.jmouse.jdbc.mapping;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetAccessor extends AbstractAccessor {

    public ResultSetAccessor(Object source) {
        super(source);
    }

    @Override
    public ObjectAccessor get(String name) {
        return null;
    }

    @Override
    public ObjectAccessor get(int index) {
        try {
            return wrap(asType(ResultSet.class).getObject(index));
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
