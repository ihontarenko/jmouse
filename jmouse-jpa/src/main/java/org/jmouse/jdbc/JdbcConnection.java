package org.jmouse.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcConnection extends AutoCloseable {

    Connection unwrap();

    boolean owned();

    @Override
    void close() throws SQLException;

    final class Default implements JdbcConnection {

        private final Connection target;
        private final boolean    owned;

        public Default(Connection target, boolean owned) {
            this.target = target;
            this.owned = owned;
        }

        @Override
        public Connection unwrap() {
            return target;
        }

        @Override
        public boolean owned() {
            return owned;
        }

        @Override
        public void close() throws SQLException {
            if (owned && target != null) {
                target.close();
            }
        }

    }

}
