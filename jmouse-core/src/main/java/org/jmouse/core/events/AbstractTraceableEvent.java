package org.jmouse.core.events;

import org.jmouse.core.trace.TraceContext;

import static org.jmouse.core.Verify.nonNull;

/**
 * Base implementation for traceable events.
 *
 * @param <T> payload type
 */
public abstract class AbstractTraceableEvent<T> extends AbstractEvent<T> implements TraceableEvent<T> {

    private final TraceContext trace;

    protected AbstractTraceableEvent(EventName name, T payload, Object caller, TraceContext trace) {
        super(name, payload, caller);
        this.trace = nonNull(trace, "trace");
    }

    @Override
    public TraceContext trace() {
        return trace;
    }
}
