package org.jmouse.jdbc.connection.datasource;

import org.jmouse.core.Verify;

import javax.sql.DataSource;

public final class SimpleDataSourceResolver implements DataSourceResolver {

    private final DataSourceSpecificationRegistry specificationRegistry;
    private final DataSourceRegistry              dataSourceRegistry;
    private final DataSourceFactoryRegistry       factoryRegistry;

    public SimpleDataSourceResolver(
            DataSourceSpecificationRegistry specificationRegistry,
            DataSourceRegistry dataSourceRegistry,
            DataSourceFactoryRegistry factoryRegistry
    ) {
        this.specificationRegistry = Verify.nonNull(specificationRegistry, "specificationRegistry");
        this.factoryRegistry = Verify.nonNull(factoryRegistry, "factoryRegistry");
        this.dataSourceRegistry = Verify.nonNull(dataSourceRegistry, "dataSourceRegistry");
    }

    @Override
    public DataSource resolve(String name) {
        DataSource existing = dataSourceRegistry.obtain(name);

        if (existing != null) {
            return existing;
        }

        synchronized (dataSourceRegistry) {
            existing = dataSourceRegistry.obtain(name);

            if (existing != null) {
                return existing;
            }

            DataSourceSpecification specification = specificationRegistry.require(name);
            DataSource              dataSource    = factoryRegistry.create(specification);

            dataSourceRegistry.register(name, dataSource);

            return dataSource;
        }
    }
}
