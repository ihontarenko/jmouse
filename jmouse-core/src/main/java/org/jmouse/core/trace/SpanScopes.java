package org.jmouse.core.trace;

import org.jmouse.core.context.ExecutionContext;
import org.jmouse.core.context.ExecutionContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public final class SpanScopes {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpanScopes.class);

    private SpanScopes() {}

    public static <T> T none(Supplier<T> action) {
        return action.get();
    }

    public static <T> T rootIfAbsent(Supplier<T> action) {
        ExecutionContext context = ExecutionContextHolder.current();
        TraceContext     trace   = context.get(TraceKeys.TRACE);

        if (trace != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("rootIfAbsent(): trace already present -> {}", shortTrace(trace));
            }
            return action.get();
        }

        TraceContext root = TraceContext.root();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rootIfAbsent(): no trace -> open root {}", shortTrace(root));
        }

        try (ExecutionContextHolder.Scope ignored =
                     ExecutionContextHolder.open(context.with(TraceKeys.TRACE, root))) {
            return action.get();
        }
    }

    public static <T> T child(Supplier<T> action) {
        ExecutionContext context = ExecutionContextHolder.current();
        TraceContext     trace   = context.get(TraceKeys.TRACE);
        TraceContext     next    = (trace == null) ? TraceContext.root() : trace.child();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("child(): {} -> open {}", shortTrace(trace), shortTrace(next));
        }

        try (ExecutionContextHolder.Scope ignored =
                     ExecutionContextHolder.open(context.with(TraceKeys.TRACE, next))) {
            return action.get();
        }
    }

    public static void rootIfAbsent(Runnable action) {
        rootIfAbsent(() -> {
            action.run();
            return null;
        });
    }

    public static void child(Runnable action) {
        child(() -> {
            action.run();
            return null;
        });
    }

    private static String shortTrace(TraceContext trace) {
        if (trace == null) {
            return "trace=null";
        }
        return "trace[correlationId=%s, spanId=%s, parentSpanId=%s, depth=%d]".formatted(
                trace.correlationId(), trace.spanId(), trace.parentSpanId(), trace.depth()
        );
    }
}
