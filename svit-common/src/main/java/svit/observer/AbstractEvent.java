package svit.observer;

import java.util.Objects;

/**
 * An abstract base implementation of the {@link Event} interface, providing
 * common functionality for name, payload, and optional caller. Subclasses can
 * extend this class to define more specialized event behaviors or additional
 * fields and methods.
 * @see Event
 */
public abstract class AbstractEvent<T> implements Event<T> {

    /**
     * The unique name identifying this event, used by observers or handlers
     * to recognize and filter events.
     */
    protected final String name;

    /**
     * The payload containing data relevant to this event.
     * Could be any object type depending on the event's purpose.
     */
    protected final T payload;

    /**
     * An optional reference to the source or origin of this event.
     */
    protected final Object caller;

    /**
     * Constructs a new {@code AbstractEvent} with the specified event name,
     * payload, and caller.
     *
     * @param name    the name identifying this event
     * @param payload the payload containing data relevant to this event
     * @param caller  an optional reference to the source or origin of this event
     */
    public AbstractEvent(String name, T payload, Object caller) {
        this.name = name;
        this.payload = payload;
        this.caller = caller;
    }

    /**
     * Constructs a new {@code AbstractEvent} with the specified event name
     * and payload. The caller is set to {@code null}.
     *
     * @param name    the name identifying this event
     * @param payload the payload containing data relevant to this event
     */
    public AbstractEvent(String name, T payload) {
        this(name, payload, null);
    }

    /**
     * The unique name identifying this event, often used by observers to filter
     * or handle specific events.
     *
     * @return the name of this event
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * The payload of this event, containing data relevant to observers. Could be any
     * object type, such as domain models, messages, or other event-specific structures.
     *
     * @return the payload of this event
     */
    @Override
    public T payload() {
        return payload;
    }

    /**
     * Returns the {@code Class} object representing the type of the payload.
     * Useful for determining how to process or cast the payload.
     * <p>
     * By default, this method uses {@link Objects#requireNonNull(Object)}
     * on the payload to ensure that the payload is not {@code null}, and
     * then returns its class. If the payload is {@code null}, an exception
     * will be thrown.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends T> payloadType() {
        return (Class<? extends T>) Objects.requireNonNull(payload()).getClass();
    }

    /**
     * An optional reference to the source or origin of this event. This could be
     * the publisher itself or another object providing context for the event.
     *
     * @return the caller or source of this event, or {@code null} if not provided
     */
    @Override
    public Object caller() {
        return caller;
    }
}
