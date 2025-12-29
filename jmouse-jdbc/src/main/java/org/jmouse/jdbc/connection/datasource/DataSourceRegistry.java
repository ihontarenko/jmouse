package org.jmouse.jdbc.connection.datasource;

import org.jmouse.core.Contract;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DataSourceRegistry {

    private final Map<String, DataSource> dataSources = new LinkedHashMap<>();

    public void register(String name, DataSource dataSource) {
        Contract.nonNull(name, "name");
        Contract.nonNull(dataSource, "dataSource");
        Contract.state(dataSources.putIfAbsent(name, dataSource) != null,
                       "DATA-SOURCE already registered: " + name);
    }

    public DataSource obtain(String name) {
        return dataSources.get(name);
    }

    public DataSource require(String name) {
        return Contract.state(obtain(name), "DATA-SOURCE NOT FOUND: " + name);
    }

    public Map<String, DataSource> snapshot() {
        return Map.copyOf(dataSources);
    }

}
