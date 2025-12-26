package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MapRowMapper implements RowMapper<Map<String, Object>> {

    @Override
    public Map<String, Object> map(ResultSet resultSet) throws SQLException {
        RowMetadata         metadata = ResultSetRowMetadata.of(resultSet);
        Map<String, Object> map      = new HashMap<>();

        for (String name : metadata.names()) {
            map.put(name, resultSet.getObject(name));
        }

        return map;
    }

}
