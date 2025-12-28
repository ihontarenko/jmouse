package org.jmouse.jdbc.intercept.link;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.proxy.Bubble;
import org.jmouse.jdbc.exception.DataAccessException;
import org.jmouse.jdbc.exception.SQLExceptionTranslator;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;

import java.sql.SQLException;

public final class JdbcExceptionTranslationLink implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    private final SQLExceptionTranslator translator;

    public JdbcExceptionTranslationLink(SQLExceptionTranslator translator) {
        this.translator = translator;
    }

    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> call, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {
        try {
            return next.proceed(context, call);
        } catch (Throwable exception) {
            SQLException sqlException = null;

            if (exception instanceof SQLException) {
                sqlException = (SQLException) exception;
            }

            if (exception instanceof Bubble bubble && bubble.getCause() instanceof SQLException) {
                sqlException = (SQLException) bubble.getCause();
            }

            if (sqlException != null) {
                throw translator.translate(call.operation().name(), call.sql(), sqlException);
            }

            throw new DataAccessException("SQL Exception", exception);
        }
    }
}
