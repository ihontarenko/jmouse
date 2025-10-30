package org.jmouse.jdbc;

import org.jmouse.tx.JtaResourceContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider {

    JdbcConnection acquire() throws SQLException;

    /**
     * 📦 Default provider: prefers thread-bound connection, else opens from DataSource.
     */
    final class Default implements ConnectionProvider {
        private final javax.sql.DataSource dataSource;
        private final JtaResourceContext   resourceContext;
        private final Object               key;

        public Default(javax.sql.DataSource dataSource) {
            this(dataSource, JtaResourceContext::get, DataSourceKey.of(dataSource));
        }

        public Default(DataSource dataSource, JtaResourceContext ctx, Object key) {
            this.dataSource = dataSource;
            this.resourceContext = ctx;
            this.key = key;
        }

        @Override
        public JdbcConnection acquire() throws SQLException {
            Connection bound = JtaResourceContext.get(key);

            if (bound != null) {
                return new JdbcConnection.Default(bound, false);
            }

            Connection fresh = dataSource.getConnection();

            return new JdbcConnection.Default(fresh, true);
        }
    }

    /**
     * 📛 Small key wrapper for binding by DataSource identity.
     */
    final class DataSourceKey {

        private record IdentityKey(Object reference) { }

        public static Object of(DataSource dataSource) {
            return new IdentityKey(dataSource);
        }
    }

}