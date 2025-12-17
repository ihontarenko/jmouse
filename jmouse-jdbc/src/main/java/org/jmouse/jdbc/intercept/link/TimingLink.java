package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimingLink implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimingLink.class);

    private final long threshold;

    public TimingLink(long threshold) {
        this.threshold = threshold;
    }

    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> input, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {
        long t0 = System.nanoTime();
        try {
            return next.proceed(context, input);
        } finally {
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
            if (elapsedMs >= threshold) {
                LOGGER.warn("[JDBC] SLOW ({} ms): {}", elapsedMs, input.getSql());
            }
        }
    }
}
