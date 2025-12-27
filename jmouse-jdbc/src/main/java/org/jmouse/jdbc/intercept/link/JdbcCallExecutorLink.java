package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.jdbc.core.JdbcExecutor;
import org.jmouse.jdbc.core.exception.DataAccessException;
import org.jmouse.jdbc.intercept.*;

import java.sql.SQLException;

public final class JdbcCallExecutorLink implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> call, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {
        JdbcExecutor delegate = context.delegate();

        try {
            Object result = switch (call) {
                case JdbcQueryCall<?> q ->
                        delegate.execute(q.sql(), q.binder(), q.statementCallback(), q.extractor());
                case JdbcUpdateCall u ->
                        delegate.executeUpdate(u.sql(), u.binder());
                case JdbcBatchUpdateCall b ->
                        delegate.executeBatch(b.sql(), b.binders());
                case JdbcKeyUpdateCall<?> k ->
                        delegate.executeUpdateWithKey(k.sql(), k.binder(), k.keyExtractor());
            };
            return Outcome.done(result);
        } catch (SQLException e) {
            throw new DataAccessException("Failed during jdbc-call execution.", e);
        }
    }

}
