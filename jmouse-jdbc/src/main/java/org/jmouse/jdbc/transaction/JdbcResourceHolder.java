package org.jmouse.jdbc.transaction;

import java.sql.Connection;

public final class JdbcResourceHolder {

    private final Connection connection;

    public JdbcResourceHolder(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }
}

