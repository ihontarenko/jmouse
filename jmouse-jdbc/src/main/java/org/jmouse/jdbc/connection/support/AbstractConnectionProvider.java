package org.jmouse.jdbc.connection.support;

import org.jmouse.jdbc.JdbcSupport;
import org.jmouse.jdbc.connection.ConnectionProvider;

import java.sql.Connection;

abstract public class AbstractConnectionProvider implements ConnectionProvider {

    @Override
    public void release(Connection connection) {
        JdbcSupport.closeQuietly(connection);
    }

}
