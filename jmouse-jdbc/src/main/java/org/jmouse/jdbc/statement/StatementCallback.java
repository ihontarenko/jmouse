package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface StatementCallback<T> {

    StatementCallback<ResultSet> QUERY  = PreparedStatement::executeQuery;
    StatementCallback<Integer>   UPDATE = PreparedStatement::executeUpdate;
    StatementCallback<int[]>     BATCH  = PreparedStatement::executeBatch;

    T doStatementExecute(PreparedStatement statement) throws SQLException;

}
