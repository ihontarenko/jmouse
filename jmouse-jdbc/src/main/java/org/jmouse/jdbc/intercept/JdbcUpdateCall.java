package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;
import org.jmouse.jdbc.statement.StatementConfigurer;

public record JdbcUpdateCall(
        String sql,
        PreparedStatementBinder binder,
        StatementConfigurer configurer,
        StatementCallback<Integer> callback
) implements JdbcCall<Integer> {

    @Override
    public JdbcOperation operation() {
        return JdbcOperation.UPDATE;
    }

    public JdbcUpdateCall with(StatementConfigurer configurer) {
        return new JdbcUpdateCall(sql, binder, StatementConfigurer.chain(this.configurer(), configurer), callback);
    }

}
