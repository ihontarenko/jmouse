package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CompositeKeyExtractor implements KeyExtractor<Map<String, Object>> {

    @Override
    public Map<String, Object> extract(ResultSet keys) throws SQLException {
        if (!keys.next()) {
            return Map.of();
        }

        Map<String, Object> result   = new LinkedHashMap<>();
        RowMetadata         metadata = ResultSetRowMetadata.of(keys);

        for (String name : metadata.names()) {
            result.put(name, keys.getObject(metadata.indexOf(name)));
        }

        return result;
    }
}
