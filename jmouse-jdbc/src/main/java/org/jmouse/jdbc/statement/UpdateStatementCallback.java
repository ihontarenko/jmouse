package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class UpdateStatementCallback
        implements StatementCallback<Integer> {

    @Override
    public Integer doWithStatement(PreparedStatement statement)
            throws SQLException {

        return statement.executeUpdate();
    }
}
