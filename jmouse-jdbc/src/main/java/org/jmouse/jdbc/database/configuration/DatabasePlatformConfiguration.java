package org.jmouse.jdbc.database.configuration;

import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.database.DatabasePlatform;
import org.jmouse.jdbc.database.DatabasePlatformProvider;
import org.jmouse.jdbc.database.DatabasePlatformRegistry;
import org.jmouse.jdbc.database.meta.DatabaseMetaPlatformResolver;
import org.jmouse.jdbc.database.standard.*;

import java.util.List;

/**
 * Core database-platform configuration for jMouse JDBC.
 */
@BeanFactories
public class DatabasePlatformConfiguration {

    @Bean
    public DatabasePlatformRegistry databasePlatformRegistry(@AggregatedBeans List<DatabasePlatformProvider> providers) {
        DatabasePlatformRegistry registry = new DatabasePlatformRegistry()
                .fallback(StandardPlatforms.ansi());

        // External modules can contribute providers via BeanContext.
        for (DatabasePlatformProvider provider : providers) {
            registry.register(provider);
        }

        return registry;
    }

    /**
     * Standard providers (can be overridden or supplemented by user modules).
     */
    @Bean
    public DatabasePlatformProvider postgresPlatformProvider() {
        return new PostgresPlatformProvider();
    }

    @Bean
    public DatabasePlatformProvider mySQLPlatformProvider() {
        return new MySQLPlatformProvider();
    }

    @Bean
    public DatabasePlatformProvider h2PlatformProvider() {
        return new H2PlatformProvider();
    }

    @Bean
    public DatabasePlatform databasePlatform(ConnectionProvider rawConnectionProvider, DatabasePlatformRegistry registry) {
        return new DatabaseMetaPlatformResolver(rawConnectionProvider, registry).resolve();
    }

}
