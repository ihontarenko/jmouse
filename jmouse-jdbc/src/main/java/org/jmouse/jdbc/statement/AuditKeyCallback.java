package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AuditKeyCallback<K> implements KeyUpdateCallback<K> {

    private int affected;

    @Override
    @SuppressWarnings("unchecked")
    public K doStatementExecute(PreparedStatement stmt, ResultSet keys)
            throws SQLException {
        affected = stmt.executeUpdate();
        return keys.next() ? (K) keys.getObject(1) : null;
    }

    public int affectedRows() {
        return affected;
    }

}
