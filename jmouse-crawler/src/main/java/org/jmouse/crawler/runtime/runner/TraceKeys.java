package org.jmouse.crawler.runtime.runner;

import org.jmouse.core.context.ContextKey;
import org.jmouse.core.trace.TraceContext;

public class TraceKeys {
    public static final ContextKey<TraceContext> TRACE = ContextKey.of("task.trace", TraceContext.class);

    private TraceKeys() {
    }
}
