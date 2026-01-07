package org.jmouse.jdbc.connection.datasource;

import org.jmouse.core.Verify;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DataSourceRegistry {

    private final Map<String, DataSource> dataSources = new LinkedHashMap<>();

    public void register(String name, DataSource dataSource) {
        Verify.nonNull(name, "name");
        Verify.nonNull(dataSource, "dataSource");
        Verify.state(dataSources.putIfAbsent(name, dataSource) == null,
                     "DATA-SOURCE already registered: " + name);
    }

    public DataSource obtain(String name) {
        return dataSources.get(name);
    }

    public DataSource require(String name) {
        return Verify.state(obtain(name), "DATA-SOURCE NOT FOUND: " + name);
    }

    public Map<String, DataSource> snapshot() {
        return Map.copyOf(dataSources);
    }

}
