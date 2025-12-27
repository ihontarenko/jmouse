package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.PreparedStatementBinder;

import java.util.List;

public record JdbcBatchUpdateCall(
        String sql,
        List<? extends PreparedStatementBinder> binders
) implements JdbcCall<int[]> {

    @Override
    public JdbcOperation operation() {
        return JdbcOperation.BATCH_UPDATE;
    }

    public int batchSize() {
        return binders == null ? 0 : binders.size();
    }
}