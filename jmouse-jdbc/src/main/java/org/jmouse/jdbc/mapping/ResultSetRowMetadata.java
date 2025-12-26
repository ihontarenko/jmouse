package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ResultSetRowMetadata implements RowMetadata {

    private final Map<String, Integer> index;

    private ResultSetRowMetadata(Map<String, Integer> index) {
        this.index = index;
    }

    public static RowMetadata of(ResultSet resultSet) {
        try {
            ResultSetMetaData    metaData = resultSet.getMetaData();
            int                  count    = metaData.getColumnCount();
            Map<String, Integer> mapping  = new HashMap<>(count * 2);

            for (int index = 1; index <= count; index++) {
                Optional<String> name  = Optional.ofNullable(metaData.getColumnName(index));
                Optional<String> label = Optional.ofNullable(metaData.getColumnLabel(index));
                int[]            cache = new int[]{index};
                name.ifPresent(n -> mapping.put(n, cache[0]));
                label.ifPresent(n -> mapping.putIfAbsent(n, cache[0]));
            }

            return new ResultSetRowMetadata(mapping);
        } catch (SQLException ignore) {

        }

        return new ResultSetRowMetadata(Collections.emptyMap());
    }


    @Override
    public Map<String, Integer> indexMap() {
        return Map.copyOf(index);
    }

}
