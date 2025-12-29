package org.jmouse.jdbc.connection.datasource.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Base {@link javax.sql.DataSource} for driver-based implementations.
 */
public abstract class AbstractDriverBasedDataSource extends AbstractDataSource {

    public static final String     USER     = "user";
    public static final String     PASSWORD = "password";
    private             String     username;
    private             String     password;
    private             String     url;
    private             String     catalog;
    private             String     schema;
    private             Properties connectionProperties;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = (url != null ? url.trim() : null);
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Properties getConnectionProperties() {
        return connectionProperties;
    }

    public void setConnectionProperties(Properties connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(getUsername(), getPassword());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Properties driverProperties = new Properties();
        Properties properties       = getConnectionProperties();

        if (properties != null) {
            driverProperties.putAll(properties);
        }
        if (username != null) {
            driverProperties.setProperty(USER, username);
        }
        if (password != null) {
            driverProperties.setProperty(PASSWORD, password);
        }

        Connection connection = getConnectionFor(driverProperties);

        if (catalog != null) {
            connection.setCatalog(catalog);
        }
        if (schema != null) {
            connection.setSchema(schema);
        }

        return connection;
    }

    protected abstract Connection getConnectionFor(Properties properties) throws SQLException;
}
