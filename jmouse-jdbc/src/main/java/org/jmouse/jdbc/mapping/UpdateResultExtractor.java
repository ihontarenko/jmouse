package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class UpdateResultExtractor
        implements ResultSetExtractor<Integer> {

    @Override
    public Integer extract(ResultSet resultSet) throws SQLException {
        return resultSet == null ? 0 : resultSet.getStatement().getUpdateCount();
    }
}
