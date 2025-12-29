package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.KeyUpdateCallback;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;

public record JdbcKeyUpdateCall<K>(
        String sql,
        PreparedStatementBinder binder,
        StatementConfigurer configurer,
        KeyUpdateCallback<K> callback
) implements JdbcCall<K> {

    @Override
    public JdbcOperation operation() {
        return JdbcOperation.UPDATE_RETURNING_KEYS;
    }

    public JdbcKeyUpdateCall<K> with(StatementConfigurer configurer) {
        return new JdbcKeyUpdateCall<>(
                sql, binder,
                StatementConfigurer.combine(this.configurer(), configurer),
                callback
        );
    }
}