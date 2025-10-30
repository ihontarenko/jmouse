package org.jmouse.jdbc._wip.config.spi;

import org.jmouse.jdbc._wip.config.DataSourceProperties;
import org.jmouse.jdbc._wip.config.support.DriverClassResolver;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * 🪙 Fallback provider using DriverManager (no pooling).
 */
public final class DriverManagerDataSourceProvider implements DataSourceProvider {

    @Override
    public String id() {
        return "driver";
    }

    @Override
    public boolean isAvailable(DataSourceProperties sourceProperties) {
        return sourceProperties.url != null;
    }

    @Override
    public DataSource create(DataSourceProperties sourceProperties) {
        DriverClassResolver.ensureDriverLoaded(sourceProperties);
        return new SimpleDriverManagerDataSource(
                sourceProperties.url, sourceProperties.username, sourceProperties.password);
    }

    /**
     * Minimal DataSource backed by DriverManager.
     */
    static final class SimpleDriverManagerDataSource implements DataSource {
        private final String url, username, password;

        SimpleDriverManagerDataSource(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return (username != null)
                    ? DriverManager.getConnection(url, username, password)
                    : DriverManager.getConnection(url);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        // boilerplate no-ops:
        @Override
        public <T> T unwrap(Class<T> iface) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return new PrintWriter(System.out);
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }
    }
}
