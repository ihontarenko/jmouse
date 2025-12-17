package org.jmouse.jdbc.platform;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * DriverManager-based platform configuration.
 */
public record DriverManagerPlatformConfig(
        String jdbcUrl,
        Optional<String> username,
        Optional<String> password,
        Properties properties
) {
    public DriverManagerPlatformConfig {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(properties, "properties");
    }

    public static DriverManagerPlatformConfig ofUrl(String jdbcUrl) {
        return new DriverManagerPlatformConfig(jdbcUrl, Optional.empty(), Optional.empty(), new Properties());
    }

    public static DriverManagerPlatformConfig ofCredentials(String jdbcUrl, String username, String password) {
        return new DriverManagerPlatformConfig(jdbcUrl, Optional.ofNullable(username), Optional.ofNullable(password), new Properties());
    }
}