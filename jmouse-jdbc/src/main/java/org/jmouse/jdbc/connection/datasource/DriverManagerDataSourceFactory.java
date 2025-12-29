package org.jmouse.jdbc.connection.datasource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DriverManagerDataSourceFactory implements DataSourceFactory {

    @Override
    public boolean supports(DataSourceSpecification specification) {
        // simplest fallback
        return true;
    }

    @Override
    public DataSource create(DataSourceSpecification specification) {
        return new DriverManagerDataSource(specification.url(), specification.username(), specification.password());
    }

    static final class DriverManagerDataSource implements DataSource {

        private final String url;
        private final String username;
        private final String password;

        DriverManagerDataSource(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        @Override public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }
        @Override public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override public java.io.PrintWriter getLogWriter() { throw new UnsupportedOperationException(); }
        @Override public void setLogWriter(java.io.PrintWriter out) { throw new UnsupportedOperationException(); }
        @Override public void setLoginTimeout(int seconds) { throw new UnsupportedOperationException(); }
        @Override public int getLoginTimeout() { throw new UnsupportedOperationException(); }
        @Override public java.util.logging.Logger getParentLogger() { throw new UnsupportedOperationException(); }
        @Override public <T> T unwrap(Class<T> iface) { throw new UnsupportedOperationException(); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}

