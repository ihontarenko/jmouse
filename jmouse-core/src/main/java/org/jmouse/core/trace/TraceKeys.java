package org.jmouse.core.trace;

import org.jmouse.core.context.ContextKey;

public final class TraceKeys {
    public static final ContextKey<TraceContext> TRACE = ContextKey.of("trace", TraceContext.class);

    private TraceKeys() {
    }
}
