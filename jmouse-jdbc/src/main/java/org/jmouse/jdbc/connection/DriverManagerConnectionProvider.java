package org.jmouse.jdbc.connection;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.JdbcSupport;
import org.jmouse.jdbc.connection.support.AbstractConnectionProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DriverManagerConnectionProvider extends AbstractConnectionProvider {

    public static final String URL_PARAMETER        = "url";
    public static final String PROPERTIES_PARAMETER = "properties";
    public static final String USER_PARAMETER       = "user";
    public static final String PASSWORD_PARAMETER   = "password";

    private final String     url;
    private final Properties properties;

    public DriverManagerConnectionProvider(String url, Properties properties) {
        this.url = Contract.nonNull(url, URL_PARAMETER);
        this.properties = Contract.nonNull(properties, PROPERTIES_PARAMETER);
    }

    public static DriverManagerConnectionProvider of(String url, String user, String password) {
        Properties properties = new Properties();
        properties.setProperty(USER_PARAMETER, Contract.nonNull(user, USER_PARAMETER));
        properties.setProperty(PASSWORD_PARAMETER, Contract.nonNull(password, PASSWORD_PARAMETER));
        return new DriverManagerConnectionProvider(url, properties);
    }

    @Override
    public Connection getConnection() throws SQLException {
        JdbcSupport.ensureDriver(this.url);
        return DriverManager.getConnection(url, properties);
    }

}
