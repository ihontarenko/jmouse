package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface PreparedStatementBinder {

    void bind(PreparedStatement statement) throws SQLException;

}