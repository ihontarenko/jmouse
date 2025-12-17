package org.jmouse.jdbc.core.exception;

import java.sql.SQLException;

public final class JdbcAccessException extends RuntimeException {

    public JdbcAccessException(SQLException cause) {
        super(cause);
    }

    @Override
    public synchronized SQLException getCause() {
        return (SQLException) super.getCause();
    }
}
