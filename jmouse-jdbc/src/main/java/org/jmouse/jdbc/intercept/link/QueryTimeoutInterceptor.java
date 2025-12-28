package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.JdbcQueryCall;
import org.jmouse.jdbc.intercept.QueryTimeoutSupport;
import org.jmouse.jdbc.statement.StatementCallback;

import java.sql.ResultSet;

public final class QueryTimeoutInterceptor
        implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {
    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> call, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {

        if (call instanceof JdbcQueryCall<?> queryCall) {
            StatementCallback<ResultSet> wrapped = statement -> {
                QueryTimeoutSupport.applyIfPresent(statement);
                return queryCall.statementCallback().doWithStatement(statement);
            };
            return next.proceed(context, queryCall.with(wrapped));
        }

        return next.proceed(context, call);
    }
}