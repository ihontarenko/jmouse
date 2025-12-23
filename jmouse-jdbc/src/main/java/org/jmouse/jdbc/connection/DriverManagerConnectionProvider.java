package org.jmouse.jdbc.connection;

import org.jmouse.jdbc.connection.support.AbstractConnectionProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public final class DriverManagerConnectionProvider extends AbstractConnectionProvider {

    private final String url;
    private final Properties properties;

    public DriverManagerConnectionProvider(String url, Properties properties) {
        this.url = Objects.requireNonNull(url, "url");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public static DriverManagerConnectionProvider of(String url, String user, String password) {
        Properties p = new Properties();
        if (user != null) p.setProperty("user", user);
        if (password != null) p.setProperty("password", password);
        return new DriverManagerConnectionProvider(url, p);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, properties);
    }

}
