package org.jmouse.jdbc.connection.datasource;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.connection.datasource.support.AbstractDriverBasedDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DriverManagerDataSource extends AbstractDriverBasedDataSource {

    public DriverManagerDataSource() {
    }

    public DriverManagerDataSource(String url, String username, String password) {
        setUrl(Verify.nonNull(url, "url"));
        setUsername(username);
        setPassword(password);
    }

    @Override
    protected Connection getConnectionFor(Properties properties) throws SQLException {
        return DriverManager.getConnection(getUrl(), properties);
    }

}
