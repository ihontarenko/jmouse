package org.jmouse.jdbc.statement;

import org.jmouse.jdbc.mapping.KeyExtractor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class InsertThenFetchCallback<K> implements KeyUpdateCallback<K> {

    private final KeyExtractor<K> extractor;

    public InsertThenFetchCallback(KeyExtractor<K> extractor) {
        this.extractor = extractor;
    }

    @Override
    public K doStatementExecute(PreparedStatement stmt, ResultSet keys) throws SQLException {
        int updated = stmt.executeUpdate();

        if (updated == 0) {
            throw new SQLException("Insert failed, no rows affected");
        }

        return extractor.extract(keys);
    }
}
