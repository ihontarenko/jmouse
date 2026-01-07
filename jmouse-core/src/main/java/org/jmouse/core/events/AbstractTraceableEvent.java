package org.jmouse.core.events;

import static org.jmouse.core.Verify.nonNull;

/**
 * Base implementation for traceable events.
 *
 * @param <T> payload type
 */
public abstract class AbstractTraceableEvent<T> extends AbstractEvent<T> implements TraceableEvent<T> {

    private final EventTrace trace;

    protected AbstractTraceableEvent(String name, T payload, Object caller, EventTrace trace) {
        super(name, payload, caller);
        this.trace = nonNull(trace, "trace");
    }

    @Override
    public EventTrace trace() {
        return trace;
    }
}
