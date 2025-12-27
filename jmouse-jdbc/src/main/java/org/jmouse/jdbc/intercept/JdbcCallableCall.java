package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.CallableCallback;
import org.jmouse.jdbc.statement.CallableStatementBinder;

public record JdbcCallableCall<T>(
        String sql,
        CallableStatementBinder binder,
        CallableCallback<T> callback
) implements JdbcCall<T> {
    @Override
    public JdbcOperation operation() {
        return JdbcOperation.CALL;
    }
}
