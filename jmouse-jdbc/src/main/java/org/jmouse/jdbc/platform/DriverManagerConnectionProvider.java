package org.jmouse.jdbc.platform;

import org.jmouse.jdbc.connection.ConnectionProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DriverManagerConnectionProvider implements ConnectionProvider {

    private final DriverManagerPlatformConfig config;

    public DriverManagerConnectionProvider(DriverManagerPlatformConfig config) {
        this.config = config;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Properties properties = new Properties();

        properties.putAll(config.properties());

        config.username().ifPresent(u -> properties.setProperty("user", u));
        config.password().ifPresent(p -> properties.setProperty("password", p));

        if (properties.isEmpty()) {
            return DriverManager.getConnection(config.jdbcUrl());
        }

        return DriverManager.getConnection(config.jdbcUrl(), properties);
    }

    @Override
    public void release(Connection connection) throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
