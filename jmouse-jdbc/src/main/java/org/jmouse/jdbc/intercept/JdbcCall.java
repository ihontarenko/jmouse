package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;

public final class JdbcCall<T> {

    private String sql;

    private final PreparedStatementBinder binder;
    private final StatementCallback<?> callback;
    private final ResultSetExtractor<T> extractor;
    private final JdbcOperation operation;

    public JdbcCall(
            String sql,
            PreparedStatementBinder binder,
            StatementCallback<?> callback,
            ResultSetExtractor<T> extractor,
            JdbcOperation operation
    ) {
        this.sql = sql;
        this.binder = binder;
        this.callback = callback;
        this.extractor = extractor;
        this.operation = operation;
    }

    public String getSql() { return sql; }

    public void setSql(String sql) { this.sql = sql; }

    public PreparedStatementBinder getBinder() { return binder; }

    public StatementCallback<?> getCallback() { return callback; }

    public ResultSetExtractor<T> getExtractor() { return extractor; }

    public JdbcOperation getOperation() { return operation; }
}
