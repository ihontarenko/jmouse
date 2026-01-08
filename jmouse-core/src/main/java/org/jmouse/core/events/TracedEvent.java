package org.jmouse.core.events;

import org.jmouse.core.trace.TraceContext;

public final class TracedEvent<T> implements TraceableEvent<T> {

    private final Event<T>     event;
    private final TraceContext trace;

    /**
     * Creates a traced event wrapping the given delegate event.
     *
     * @param event the original event to wrap
     * @param trace trace metadata to associate with the event
     */
    public TracedEvent(Event<T> event, TraceContext trace) {
        this.event = event;
        this.trace = trace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventName name() {
        return event.name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T payload() {
        return event.payload();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends T> payloadType() {
        return event.payloadType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object caller() {
        return event.caller();
    }

    /**
     * Returns the trace metadata associated with this event.
     *
     * @return the event trace
     */
    @Override
    public TraceContext trace() {
        return trace;
    }

    @Override
    public String toString() {
        return "TRACED: " + event.toString();
    }

}
