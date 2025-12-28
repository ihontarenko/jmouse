package org.jmouse.jdbc.exception;

import java.sql.SQLException;

public interface SQLExceptionTranslator {
    RuntimeException translate(String task, String sql, SQLException exception);
}
