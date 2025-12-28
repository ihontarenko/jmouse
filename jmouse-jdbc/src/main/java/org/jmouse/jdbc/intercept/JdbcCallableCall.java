package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.CallableCallback;
import org.jmouse.jdbc.statement.CallableStatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;

public record JdbcCallableCall<T>(
        String sql,
        CallableStatementBinder binder,
        StatementConfigurer configurer,
        CallableCallback<T> callback
) implements JdbcCall<T> {

    @Override
    public JdbcOperation operation() {
        return JdbcOperation.CALL;
    }

    public JdbcCallableCall<T> with(StatementConfigurer configurer) {
        return new JdbcCallableCall<>(
                sql, binder,
                StatementConfigurer.chain(this.configurer(), configurer),
                callback
        );
    }
}