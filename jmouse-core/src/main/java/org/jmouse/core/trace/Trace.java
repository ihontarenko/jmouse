package org.jmouse.core.trace;

import org.jmouse.core.context.execution.ExecutionContext;
import org.jmouse.core.context.execution.ExecutionContextHolder;

public final class Trace {

    private Trace() {
    }

    public static TraceContext current() {
        return ExecutionContextHolder.current().get(TraceKeys.TRACE);
    }

    public static ExecutionContext root() {
        ExecutionContext context = ExecutionContextHolder.current();
        TraceContext     current = context.get(TraceKeys.TRACE);
        return (current != null) ? context : context.with(TraceKeys.TRACE, TraceContext.root());
    }

    public static ExecutionContext child() {
        ExecutionContext context = ExecutionContextHolder.current();
        TraceContext     current = context.get(TraceKeys.TRACE);
        TraceContext     next    = (current == null) ? TraceContext.root() : current.child();
        return context.with(TraceKeys.TRACE, next);
    }

    public static ExecutionContext touch() {
        ExecutionContext context = ExecutionContextHolder.current();
        TraceContext     current = context.get(TraceKeys.TRACE);
        return (current == null) ? context : context.with(TraceKeys.TRACE, current.touch());
    }

}
