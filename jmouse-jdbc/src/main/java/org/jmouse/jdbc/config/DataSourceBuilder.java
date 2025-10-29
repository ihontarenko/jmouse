package org.jmouse.jdbc.config;

import org.jmouse.jdbc.config.spi.DriverManagerDataSourceProvider;
import org.jmouse.jdbc.config.spi.DataSourceProvider;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.*;

/**
 * 🏗️ Builds a {@link DataSource} from properties using SPI providers (pools) or DriverManager fallback.
 */
public final class DataSourceBuilder {

    private DataSourceBuilder() {}

    public static DataSource build(DataSourceProperties sourceProperties) {
        if (sourceProperties.jndiName != null && !sourceProperties.jndiName.isBlank()) {
            DataSource jndi = jndiLookup(sourceProperties.jndiName);

            if (jndi != null) {
                return jndi;
            }

            throw new IllegalStateException(
                    "JNDI DataSource not found: %s".formatted(
                            sourceProperties.jndiName));
        }

        ServiceLoader<DataSourceProvider> loader    = ServiceLoader.load(DataSourceProvider.class);
        List<DataSourceProvider>          providers = new ArrayList<>();
        loader.iterator().forEachRemaining(providers::add);

        if (sourceProperties.poolName != null && !sourceProperties.poolName.isBlank()) {
            for (DataSourceProvider provider : providers) {
                if (sourceProperties.poolName.equalsIgnoreCase(provider.id())
                        && provider.isAvailable(sourceProperties)) {
                    return provider.create(sourceProperties);
                }
            }
            throw new IllegalStateException("Requested pool '" + sourceProperties.poolName + "' not available on classpath.");
        }

        // Else: pick first available provider (classpath-driven), else fallback
        for (DataSourceProvider provider : providers) {
            if (provider.isAvailable(sourceProperties)) {
                return provider.create(sourceProperties);
            }
        }

        // Fallback: DriverManager (no pool)
        return new DriverManagerDataSourceProvider().create(sourceProperties);
    }

    // Optional: lightweight JNDI (only if app server provides it)
    private static DataSource jndiLookup(String name) {
        try {
            InitialContext initialContext = new InitialContext();
            if (initialContext.lookup(name) instanceof DataSource dataSource) {
                return dataSource;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }
}
