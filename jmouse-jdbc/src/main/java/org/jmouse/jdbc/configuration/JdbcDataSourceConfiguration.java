package org.jmouse.jdbc.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.Sorter;
import org.jmouse.jdbc.connection.datasource.*;

@BeanFactories
public class JdbcDataSourceConfiguration {

    @Bean
    @org.jmouse.context.BeanIfTypeAbsent(DataSource.class)
    public DataSource defaultDataSource(DataSourceResolver resolver, DataSourceKey sourceKey) {
        return new ResolvableDataSource(resolver, sourceKey);
    }

    @Bean
    public DataSourceSpecificationRegistry dataSourceSpecificationRegistry(
        Set<DataSourceContributor> contributors
    ) {
        DataSourceSpecificationRegistry registry = new DataSourceSpecificationRegistry();
        List<DataSourceContributor> sorted = new ArrayList<>(contributors);

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
    public DataSourceFactoryRegistry dataSourceFactoryRegistry(List<DataSourceFactory> factories) {
        DataSourceFactoryRegistry registry = new DataSourceFactoryRegistry();
        List<DataSourceFactory> sorted = new ArrayList<>(factories);

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
    @org.jmouse.beans.annotation.PrimaryBean
    public DataSourceResolver dataSourceResolver(
        DataSourceSpecificationRegistry specifications,
        DataSourceRegistry resolved,
        DataSourceFactoryRegistry factories
    ) {
        return new SimpleDataSourceResolver(specifications, resolved, factories);
    }

    @Bean
    public DataSourceFactory driverInstanceDataSourceFactory() {
        return new DriverInstanceDataSourceFactory();
    }

    @Bean(scope = BeanScope.PROTOTYPE)
    public DataSourceKey dataSourceKey() {
        return DataSourceKeyHolder::current;
    }
}
