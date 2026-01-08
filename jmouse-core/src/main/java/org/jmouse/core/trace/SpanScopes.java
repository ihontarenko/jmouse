package org.jmouse.core.trace;

import org.jmouse.core.context.ExecutionContext;
import org.jmouse.core.context.ExecutionContextHolder;

import java.util.function.Supplier;

public final class SpanScopes {

    private SpanScopes() {}

    public static <T> T rootIfAbsent(Supplier<T> action) {
        ExecutionContext context = ExecutionContextHolder.current();
        TraceContext     trace   = context.get(TraceKeys.TRACE);

        if (trace != null) {
            return action.get();
        }

        TraceContext root = TraceContext.root();

        try (ExecutionContextHolder.Scope ignored =
                     ExecutionContextHolder.open(context.with(TraceKeys.TRACE, root))) {
            return action.get();
        }
    }

    public static <T> T child(Supplier<T> action) {
        ExecutionContext context = ExecutionContextHolder.current();
        TraceContext     trace   = context.get(TraceKeys.TRACE);
        TraceContext     next    = (trace == null) ? TraceContext.root() : trace.child();

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
}
