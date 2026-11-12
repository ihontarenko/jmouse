package org.jmouse.jdbc.connection.datasource;

import java.time.Duration;
import java.util.Properties;

/** Optional configuration-factory input; applications may supply any DataSource directly instead. */
public record DataSourceSpecification(
    String name,
    String driverClassName,
    String url,
    String username,
    String password,
    String catalog,
    String schema,
    Pool pool,
    Properties properties
) {
    public DataSourceSpecification {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("DataSource name must not be blank");
        }
        properties = copy(properties);
    }

    @Override
    public Properties properties() {
        return copy(properties);
    }

    /** URLs and driver properties may contain credentials and must not appear in diagnostics. */
    @Override
    public String toString() {
        return (
            "DataSourceSpecification[name=" +
            name +
            ", driverClassName=" +
            driverClassName +
            ", pooled=" +
            (pool != null) +
            "]"
        );
    }

    private static Properties copy(Properties properties) {
        if (properties == null) {
            return null;
        }
        Properties copy = new Properties();
        properties.forEach(copy::put);
        for (String name : properties.stringPropertyNames()) {
            copy.setProperty(name, properties.getProperty(name));
        }
        return copy;
    }

    public record Pool(
        int minSize,
        int maxSize,
        Duration connectionTimeout,
        Duration idleTimeout,
        Duration maxLifetime
    ) {
        public Pool {
            if (minSize < 0 || maxSize <= 0 || minSize > maxSize) {
                throw new IllegalArgumentException("Invalid connection pool size range");
            }
            requireNonNegative(connectionTimeout);
            requireNonNegative(idleTimeout);
            requireNonNegative(maxLifetime);
        }

        private static void requireNonNegative(Duration duration) {
            if (duration != null && duration.isNegative()) {
                throw new IllegalArgumentException("Pool duration must not be negative");
            }
        }
    }
}
