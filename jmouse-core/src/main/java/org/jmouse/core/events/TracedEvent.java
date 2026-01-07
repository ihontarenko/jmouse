package org.jmouse.core.events;

/**
 * Wraps a non-traceable event with trace metadata.
 */
public final class TracedEvent<T> implements TraceableEvent<T> {

    private final Event<T>   delegate;
    private final EventTrace trace;

    public TracedEvent(Event<T> delegate, EventTrace trace) {
        this.delegate = delegate;
        this.trace = trace;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public T payload() {
        return delegate.payload();
    }

    @Override
    public Class<? extends T> payloadType() {
        return delegate.payloadType();
    }

    @Override
    public Object caller() {
        return delegate.caller();
    }

    @Override
    public EventTrace trace() {
        return trace;
    }

}