package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.proxy.Bubble;
import org.jmouse.jdbc.JdbcExecutor;
import org.jmouse.jdbc.intercept.*;

import java.sql.SQLException;

public final class JdbcCallExecutorLink implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> call, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {
        JdbcExecutor executor = context.delegate();

        try {
            return Outcome.done(switch (call) {
                case JdbcQueryCall<?> q ->
                        executor.execute(q.sql(), q.binder(), q.configurer(), q.callback(), q.extractor());
                case JdbcUpdateCall u ->
                        executor.executeUpdate(u.sql(), u.binder(), u.configurer(), u.callback());
                case JdbcBatchUpdateCall b ->
                        executor.executeBatch(b.sql(), b.binders(), b.configurer(), b.callback());
                case JdbcKeyUpdateCall<?> k ->
                        executor.executeUpdate(k.sql(), k.binder(), k.configurer(), k.callback());
                case JdbcCallableCall<?> c ->
                        executor.executeCall(c.sql(), c.binder(), c.configurer(), c.callback());
            });
        } catch (SQLException e) {
            throw new Bubble(e);
        }
    }

}
