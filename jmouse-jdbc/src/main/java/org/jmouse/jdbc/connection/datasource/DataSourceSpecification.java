package org.jmouse.jdbc.connection.datasource;

import java.time.Duration;
import java.util.Properties;

public record DataSourceSpecification(
        String name,
        String driverClassName,
        String url,
        String username,
        String password,
        String catalog,
        String schema,
        Pool pool,               // optional
        Properties properties
) {
    public record Pool(
            int minSize,
            int maxSize,
            Duration connectionTimeout,
            Duration idleTimeout,
            Duration maxLifetime
    ) { }
}
