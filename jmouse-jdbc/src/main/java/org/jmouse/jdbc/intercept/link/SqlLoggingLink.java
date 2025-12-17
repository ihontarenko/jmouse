package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SqlLoggingLink implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SqlLoggingLink.class);

    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> input, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {
        LOGGER.info("[JDBC] SQL: {}", input.getSql());
        return Outcome.next();
    }

}
