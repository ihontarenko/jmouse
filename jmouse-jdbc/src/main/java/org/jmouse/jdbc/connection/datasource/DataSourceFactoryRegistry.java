package org.jmouse.jdbc.connection.datasource;

import org.jmouse.core.Contract;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DataSourceFactoryRegistry implements DataSourceFactory {

    private final List<DataSourceFactory> factories = new ArrayList<>();

    public DataSourceFactoryRegistry register(DataSourceFactory factory) {
        factories.add(Contract.nonNull(factory, "factory"));
        return this;
    }

    @Override
    public boolean supports(DataSourceSpecification specification) {
        return !factories.isEmpty();
    }

    @Override
    public DataSource create(DataSourceSpecification specification) {
        return factories.stream()
                .filter(factory -> factory.supports(specification))
                .max(Comparator.comparingInt(DataSourceFactory::priority))
                .orElseThrow(() -> new IllegalStateException("No data-source-factory for: " + specification.name()))
                .create(specification);
    }
}

