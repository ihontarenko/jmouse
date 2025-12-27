package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.core.KeyExtractor;
import org.jmouse.jdbc.statement.PreparedStatementBinder;

public record JdbcKeyUpdateCall<K>(
        String sql,
        PreparedStatementBinder binder,
        KeyExtractor<K> keyExtractor
) implements JdbcCall<K> {
    @Override
    public JdbcOperation operation() {
        return JdbcOperation.UPDATE_RETURNING_KEYS;
    }
}