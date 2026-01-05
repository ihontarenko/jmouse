package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface KeyUpdateCallback<K> {

    K doStatementExecute(PreparedStatement statement, ResultSet generatedKeys) throws SQLException;

}
