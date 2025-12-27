package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;

import java.sql.ResultSet;

public record JdbcQueryCall<T>(
        String sql,
        PreparedStatementBinder binder,
        StatementCallback<ResultSet> statementCallback,
        ResultSetExtractor<T> extractor
) implements JdbcCall<T> {

    @Override
    public JdbcOperation operation() {
        return JdbcOperation.QUERY;
    }

}
