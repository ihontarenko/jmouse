package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.KeyUpdateCallback;
import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

public record JdbcKeyUpdateCall<K>(
        String sql,
        StatementBinder binder,
        StatementConfigurer configurer,
        StatementHandler<K> handler,
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
                handler,
                callback
        );
    }
}