package org.jmouse.jdbc.connection;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider {

    Connection getConnection() throws SQLException;

    void release(Connection connection);

}