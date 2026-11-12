package org.jmouse.jdbc.connection.support;

import java.sql.Connection;
import java.sql.SQLException;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.exception.JdbcAccessException;

/** Default release returns a connection to its owner/pool by closing it. Failures are observable. */
public abstract class AbstractConnectionProvider implements ConnectionProvider {

    @Override
    public void release(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new JdbcAccessException(exception);
        }
    }
}
