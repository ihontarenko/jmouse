package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.proxy.Bubble;
import org.jmouse.jdbc.JdbcExecutor;
import org.jmouse.jdbc.intercept.*;

import java.sql.SQLException;

/**
 * Terminal chain link that executes a {@link JdbcCall} against the underlying {@link JdbcExecutor}.
 * <p>
 * This link is intended to be placed at (or near) the end of an interception chain.
 * It performs the actual JDBC invocation by switching on the concrete {@link JdbcCall}
 * subtype and delegating to the corresponding {@link JdbcExecutor} method.
 *
 * <h3>Role in the chain</h3>
 * <ul>
 *     <li>Consumes a {@link JdbcCall} and turns it into a real JDBC execution</li>
 *     <li>Produces a completed {@link Outcome} using {@link Outcome#done(Object)}</li>
 *     <li>Wraps {@link SQLException} into {@link Bubble} to propagate through proxy/interceptor layers</li>
 * </ul>
 *
 * <h3>Execution flow</h3>
 * <pre>{@code
 * Link: JdbcCallExecutorLink
 *   input:  JdbcExecutionContext + JdbcCall<?>
 *   action: delegate to JdbcExecutor (execute / update / batch / keys / call)
 *   output: Outcome.done(result)
 * }</pre>
 *
 * <p>
 * ⚠️ This link does not call {@code next}. It is a terminal executor link.
 *
 * @author jMouse
 */
public final class JdbcCallExecutorLink implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    /**
     * Executes the given {@link JdbcCall} against the {@link JdbcExecutor} stored in the context.
     * <p>
     * Any {@link SQLException} is wrapped in {@link Bubble} to preserve the original cause
     * while satisfying upstream interception/proxy contracts.
     *
     * @param context execution context carrying the delegate executor
     * @param call    immutable call descriptor
     * @param next    next chain segment (unused; this link is terminal)
     * @return completed outcome with the JDBC call result
     * @throws Bubble wraps {@link SQLException}
     */
    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> call, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {
        JdbcExecutor executor = context.delegate();

        try {
            return Outcome.done(switch (call) {
                case JdbcQueryCall<?> q ->
                        executor.execute(q.sql(), q.binder(), q.configurer(), q.handler(), q.callback(), q.extractor());
                case JdbcUpdateCall u ->
                        executor.executeUpdate(u.sql(), u.binder(), u.configurer(), u.handler(), u.callback());
                case JdbcBatchUpdateCall b ->
                        executor.executeBatch(b.sql(), b.binders(), b.configurer(), b.handler(), b.callback());
                case JdbcKeyUpdateCall<?> k ->
                        executor.executeUpdate(k.sql(), k.binder(), k.configurer(), k.handler(), k.callback());
                case JdbcCallableCall<?> c ->
                        executor.executeCall(c.sql(), c.binder(), c.configurer(), c.handler(), c.callback());
            });
        } catch (SQLException e) {
            throw new Bubble(e);
        }
    }

}
