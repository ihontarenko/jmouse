package org.jmouse.jdbc.connection.datasource;

import org.jmouse.core.Verify;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DataSourceFactoryRegistry {

    private final List<DataSourceFactory> factories = new ArrayList<>();

    public DataSourceFactoryRegistry register(DataSourceFactory factory) {
        factories.add(Verify.nonNull(factory, "factory"));
        return this;
    }

    public boolean supports(DataSourceSpecification specification) {
        return !factories.isEmpty();
    }

    public DataSource create(DataSourceSpecification specification) {
        return factories.stream()
                .filter(factory -> factory.supports(specification))
                .max(Comparator.comparingInt(DataSourceFactory::priority))
                .orElseThrow(() -> new IllegalStateException("No supported data-source-factory for: " + specification.name()))
                .create(specification);
    }
}

