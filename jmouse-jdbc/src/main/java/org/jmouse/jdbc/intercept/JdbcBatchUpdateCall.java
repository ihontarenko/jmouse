package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;
import org.jmouse.jdbc.statement.StatementConfigurer;

import java.util.List;

public record JdbcBatchUpdateCall(
        String sql,
        List<? extends PreparedStatementBinder> binders,
        StatementConfigurer configurer,
        StatementCallback<int[]> callback
) implements JdbcCall<int[]> {

    @Override
    public JdbcOperation operation() {
        return JdbcOperation.BATCH_UPDATE;
    }

    public int batchSize() {
        return binders == null ? 0 : binders.size();
    }

    public JdbcBatchUpdateCall with(StatementConfigurer configurer) {
        return new JdbcBatchUpdateCall(
                sql, binders,
                StatementConfigurer.chain(this.configurer(), configurer),
                callback
        );
    }
}