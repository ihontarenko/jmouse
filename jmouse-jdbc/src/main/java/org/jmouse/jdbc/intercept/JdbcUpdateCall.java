package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.PreparedStatementBinder;

public record JdbcUpdateCall(
        String sql,
        PreparedStatementBinder binder
) implements JdbcCall<Integer> {

    @Override
    public JdbcOperation operation() {
        return JdbcOperation.UPDATE;
    }
}
