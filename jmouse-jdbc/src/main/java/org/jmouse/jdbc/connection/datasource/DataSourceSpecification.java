package org.jmouse.jdbc.connection.datasource;

import java.time.Duration;
import java.util.Map;

public record DataSourceSpecification(
        String name,
        String driverClassName,
        String url,
        String username,
        String password,
        Pool pool,               // optional
        Map<String, String> properties
) {
    public record Pool(
            int minSize,
            int maxSize,
            Duration connectionTimeout,
            Duration idleTimeout,
            Duration maxLifetime
    ) { }
}
