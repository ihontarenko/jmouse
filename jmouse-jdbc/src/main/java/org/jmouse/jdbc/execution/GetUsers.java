package org.jmouse.jdbc.execution;

import org.jmouse.jdbc.JdbcOperation;
import org.jmouse.jdbc.mapping.MapRowMapper;
import org.jmouse.jdbc.mapping.RowMapper;

public class GetUsers implements ExecutionDescriptor {

    @Override
    public JdbcOperation operation() {
        return JdbcOperation.QUERY;
    }

    @Override
    public RowMapper<?> rowMapper() {
        return new MapRowMapper();
    }

}
