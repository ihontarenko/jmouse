package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class UpdateStatementCallback
        implements StatementCallback<Integer> {

    @Override
    public Integer doStatementExecute(PreparedStatement statement)
            throws SQLException {
        return statement.executeUpdate();
    }
}
