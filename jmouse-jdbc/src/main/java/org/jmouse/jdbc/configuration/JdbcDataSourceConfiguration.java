package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.PrimaryBean;
import org.jmouse.core.Sorter;
import org.jmouse.jdbc.connection.datasource.*;
import org.jmouse.jdbc.connection.datasource.support.RoutingDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@BeanFactories
public class JdbcDataSourceConfiguration {

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
    public DataSourceResolver dataSourceResolver(
            DataSourceSpecificationRegistry specifications,
            DataSourceRegistry resolved,
            DataSourceFactoryRegistry factories
    ) {
        return new SimpleDataSourceResolver(specifications, resolved, factories);
    }

}
