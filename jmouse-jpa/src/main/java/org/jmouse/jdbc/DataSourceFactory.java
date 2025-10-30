package org.jmouse.jdbc;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 🏗️ Builds {@link DataSource} from properties.
 */
public interface DataSourceFactory {

    DataSource create(Map<String, ?> properties);

    /**
     * 🪙 Default: DriverManager-based (no pool).
     */
    final class Default implements DataSourceFactory {
        @Override
        public DataSource create(Map<String, ?> properties) {
            String url  = (String) properties.get("jdbc.url");
            String user = (String) properties.get("jdbc.username");
            String pass = (String) properties.get("jdbc.password");
            return new DriverManagerDataSource(url, user, pass);
        }
    }

    /**
     * 🚗 Simple DriverManager-backed DataSource.
     */
    final class DriverManagerDataSource implements DataSource {

        private final String url, username, password;

        public DriverManagerDataSource(String url, String username, String password) {
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
            return null;
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
