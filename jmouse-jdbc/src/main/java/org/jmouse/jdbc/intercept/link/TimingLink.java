package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;

public final class TimingLink implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    private final long slowThresholdMillis;

    public TimingLink(long slowThresholdMillis) {
        this.slowThresholdMillis = slowThresholdMillis;
    }

    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> input, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {
        long t0 = System.nanoTime();
        try {
            return next.proceed(context, input);
        } finally {
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
            if (elapsedMs >= slowThresholdMillis) {
                System.out.println("[JDBC] SLOW (" + elapsedMs + " ms): " + input.getSql());
            }
        }
    }
}
