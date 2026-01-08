package org.jmouse.core.events;

/**
 * 🧵 Decorator that adds {@link EventTrace} metadata to an {@link Event}.
 * <p>
 * {@code TracedEvent} wraps an existing, non-traceable event and
 * exposes trace information without altering the original event
 * semantics.
 *
 * <h3>Design</h3>
 * <ul>
 *   <li>Implements {@link TraceableEvent} via delegation</li>
 *   <li>Preserves event name, payload, and caller</li>
 *   <li>Adds immutable trace metadata</li>
 * </ul>
 *
 * @param <T> event payload type
 */
public final class TracedEvent<T> implements TraceableEvent<T> {

    private final Event<T>   event;
    private final EventTrace trace;

    /**
     * Creates a traced event wrapping the given delegate event.
     *
     * @param event the original event to wrap
     * @param trace trace metadata to associate with the event
     */
    public TracedEvent(Event<T> event, EventTrace trace) {
        this.event = event;
        this.trace = trace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
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
    public EventTrace trace() {
        return trace;
    }

}
