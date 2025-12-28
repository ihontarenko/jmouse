package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.QueryTimeoutSupport;

public final class QueryTimeoutInterceptor
        implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    @Override
    public Outcome<Object> handle(
            JdbcExecutionContext context,
            JdbcCall<?> call,
            Chain<JdbcExecutionContext, JdbcCall<?>, Object> next
    ) {
        return next.proceed(context, call.with(QueryTimeoutSupport::applyIfPresent));
    }

}
