package org.jmouse.jdbc.connection.datasource;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DataSourceSpecificationRegistry {

    private final Map<String, DataSourceSpecification> specifications = new LinkedHashMap<>();

    public void register(DataSourceSpecification specification) {
        if (specifications.putIfAbsent(specification.name(), specification) != null) {
            throw new IllegalStateException("DATA-SOURCE-SPECIFICATION already registered: " + specification.name());
        }
    }

    public DataSourceSpecification obtain(String dataSourceName) {
        return specifications.get(dataSourceName);
    }

    public DataSourceSpecification require(String dataSourceName) {
        DataSourceSpecification specification = obtain(dataSourceName);

        if (specification == null) {
            throw new IllegalStateException("DATA-SOURCE-SPECIFICATION NOT FOUND: " + dataSourceName);
        }

        return specification;
    }

    public Map<String, DataSourceSpecification> snapshot() {
        return Map.copyOf(specifications);
    }

}