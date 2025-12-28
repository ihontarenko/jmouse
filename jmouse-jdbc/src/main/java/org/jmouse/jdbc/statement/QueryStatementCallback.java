package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class QueryStatementCallback implements StatementCallback<ResultSet> {

    @Override
    public ResultSet doStatementExecute(PreparedStatement statement) throws SQLException {
        return statement.executeQuery();
    }

}
