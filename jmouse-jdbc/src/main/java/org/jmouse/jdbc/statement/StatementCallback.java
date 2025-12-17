package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface StatementCallback<T> {
    T doWithStatement(PreparedStatement stmt) throws SQLException;
}
