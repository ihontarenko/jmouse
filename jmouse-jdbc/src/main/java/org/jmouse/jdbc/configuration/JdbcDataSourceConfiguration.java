package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.Sorter;
import org.jmouse.jdbc.connection.datasource.*;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@BeanFactories
public class JdbcDataSourceConfiguration {

    @Bean
    public DataSource defaultDataSource(DataSourceResolver resolver, DataSourceKey sourceKey) {
        return new ResolvableDataSource(resolver, sourceKey);
    }

    @Bean
    public DataSourceSpecificationRegistry dataSourceSpecificationRegistry(
            @AggregatedBeans List<DataSourceContributor> contributors
    ) {
        DataSourceSpecificationRegistry registry = new DataSourceSpecificationRegistry();
        List<DataSourceContributor>     sorted   = new ArrayList<>(contributors);

        Sorter.sort(sorted);

        for (DataSourceContributor contributor : sorted) {
            contributor.contribute(registry);
        }

        return registry;
    }

    @Bean
    public DataSourceRegistry dataSourceRegistry() {
        return new DataSourceRegistry();
    }

    @Bean
    public DataSourceFactoryRegistry dataSourceFactoryRegistry(
            @AggregatedBeans List<DataSourceFactory> factories
    ) {
        DataSourceFactoryRegistry registry = new DataSourceFactoryRegistry();
        List<DataSourceFactory>   sorted   = new ArrayList<>(factories);

        Sorter.sort(sorted);

        for (DataSourceFactory factory : sorted) {
            registry.register(factory);
        }

        return registry;
    }

    @Bean
    public DataSourceFactory dataSourceFactory() {
        return new DriverManagerDataSourceFactory();
    }

    @Bean
    public DataSourceResolver dataSourceResolver(
            DataSourceSpecificationRegistry specifications,
            DataSourceRegistry resolved,
            DataSourceFactoryRegistry factories
    ) {
        return new SimpleDataSourceResolver(specifications, resolved, factories);
    }

    @Bean
    public DataSourceKey dataSourceKey() {
        return DataSourceKeyHolder::current;
    }

}
