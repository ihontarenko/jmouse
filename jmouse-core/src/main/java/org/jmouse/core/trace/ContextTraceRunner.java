package org.jmouse.core.trace;

import org.jmouse.core.events.EventOperations;

import java.util.function.Supplier;

public final class ContextTraceRunner implements EventOperations.TraceScopeRunner {

    @Override
    public <T> T run(EventOperations.TraceMode mode, Supplier<T> action) {
        return switch (mode) {
            case NONE -> SpanScopes.none(action);
            case ROOT_IF_ABSENT -> SpanScopes.rootIfAbsent(action);
            case CHILD -> SpanScopes.child(action);
        };
    }
}