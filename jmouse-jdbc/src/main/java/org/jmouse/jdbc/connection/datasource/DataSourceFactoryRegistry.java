package org.jmouse.jdbc.connection.datasource;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;

/** Selects the highest-priority matching factory; equal best priorities are an error. */
public final class DataSourceFactoryRegistry {

    private final List<DataSourceFactory> factories = new CopyOnWriteArrayList<>();

    public DataSourceFactoryRegistry register(DataSourceFactory factory) {
        factories.add(Objects.requireNonNull(factory, "factory"));
        return this;
    }

    public boolean supports(DataSourceSpecification specification) {
        Objects.requireNonNull(specification, "specification");
        return factories.stream().anyMatch(factory -> factory.supports(specification));
    }

    public DataSource create(DataSourceSpecification specification) {
        Objects.requireNonNull(specification, "specification");
        DataSourceFactory selected = null;
        boolean ambiguous = false;
        for (DataSourceFactory factory : factories) {
            if (!factory.supports(specification)) {
                continue;
            }
            if (selected == null || factory.priority() > selected.priority()) {
                selected = factory;
                ambiguous = false;
            } else if (factory.priority() == selected.priority()) {
                ambiguous = true;
            }
        }
        if (selected == null) {
            throw new IllegalStateException("No DataSource factory supports: " + specification.name());
        }
        if (ambiguous) {
            throw new IllegalStateException("Ambiguous DataSource factories for: " + specification.name());
        }
        return Objects.requireNonNull(selected.create(specification), "DataSource factory returned null");
    }
}
