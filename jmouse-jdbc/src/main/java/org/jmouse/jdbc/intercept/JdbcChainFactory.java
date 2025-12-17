package org.jmouse.jdbc.intercept;

import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.core.JdbcExecutor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.StatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class JdbcChainFactory {

    private JdbcChainFactory() {}

    public static Chain<JdbcExecutionContext, JdbcCall<?>, Object> withFallback(
            Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder
    ) {
        return builder.withFallback((context, call) -> {
            JdbcExecutor delegate = context.delegate();

            try {
                if (call.getOperation() == JdbcOperation.UPDATE) {
                    return delegate.executeUpdate(call.getSql(), call.getBinder());
                }

                @SuppressWarnings("unchecked")
                Object out = delegate.execute(
                        call.getSql(),
                        call.getBinder(),
                        (StatementCallback<ResultSet>) call.getCallback(),
                        (ResultSetExtractor<Object>) call.getExtractor()
                );

                return out;

            } catch (SQLException e) {
                // поки що: пробрасываем як RuntimeException або загортаємо в твій DataAccessException
                throw new RuntimeException(e);
            }
        }).toChain();
    }
}
