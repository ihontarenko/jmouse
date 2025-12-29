package org.jmouse.jdbc._app;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.PrimaryBean;
import org.jmouse.core.Priority;
import org.jmouse.jdbc.connection.datasource.*;
import org.jmouse.jdbc.connection.datasource.support.RoutingDataSource;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.function.Supplier;

@BeanFactories
public class AppDataSourcesConfiguration {

    @Bean
    @PrimaryBean
    public DataSource dataSource(DataSourceResolver resolver, Supplier<String> dataSourceLookupKey) {
        return new RoutingDataSource(resolver, dataSourceLookupKey, false, null);
    }

    @Bean
    @Priority(0)
    public DataSourceContributor primaryDataSource() {
        return registry -> registry.register(
                new DataSourceSpecification(
                        "primary",
                        "jdbc:postgresql://localhost:5432/app",
                        "app",
                        "app",
                        null,
                        null,
                        null,
                        null,
                        new Properties()
                )
        );
    }

    @Bean
    @Priority(1)
    public DataSourceContributor analyticsDataSource() {
        return registry -> registry.register(
                new DataSourceSpecification(
                        "analytics",
                        "jdbc:postgresql://localhost:5432/analytics",
                        "analytics",
                        "analytics",
                        null,
                        null,
                        null,
                        null,
                        new Properties()
                )
        );
    }
}
