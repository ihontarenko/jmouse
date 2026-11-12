package org.jmouse.jdbc.connection.datasource;

import java.util.Objects;
import javax.sql.DataSource;

/** Compatibility adapter from specifications/factories into the same named-source registry. */
public final class SimpleDataSourceResolver implements DataSourceResolver {

    private final DataSourceSpecificationRegistry specifications;
    private final DataSourceRegistry sources;
    private final DataSourceFactoryRegistry factories;

    public SimpleDataSourceResolver(
        DataSourceSpecificationRegistry specifications,
        DataSourceRegistry sources,
        DataSourceFactoryRegistry factories
    ) {
        this.specifications = Objects.requireNonNull(specifications, "specifications");
        this.sources = Objects.requireNonNull(sources, "sources");
        this.factories = Objects.requireNonNull(factories, "factories");
    }

    @Override
    public DataSource resolve(String name) {
        return sources.obtainOrCreate(name, () -> factories.create(specifications.require(name)));
    }
}
