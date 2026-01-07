package org.jmouse.core.events;

/**
 * Represents an event in a publish-subscribe (observer) pattern. Each event has:
 * <ul>
 *   <li>A unique {@link #name() name} for identification.</li>
 *   <li>A {@link #payload() payload} which provides event-specific data.</li>
 *   <li>A {@link #payloadType() payload type} indicating the class of the payload.</li>
 *   <li>An optional {@link #caller() caller} that may represent the source or origin
 *       of the event.</li>
 * </ul>
 *
 * <p>Implementations are free to provide additional context or behavior as necessary.
 * Observers can filter or handle events based on their names, payloads, or caller.</p>
 *
 * @param <T> the type of the event's payload
 */
public interface Event<T> {

    /**
     * The unique name identifying this event, often used by observers to filter
     * or handle specific events.
     *
     * @return the name of this event
     */
    String name();

    /**
     * The payload of this event, containing data relevant to observers. Could be any
     * structured type, such as domain models, messages, or other event-specific structures.
     *
     * @return the payload of this event
     */
    T payload();

    /**
     * Returns the {@code Class} structured representing the type of the payload.
     * Useful for determining how to process or cast the payload.
     *
     * @return the class of the payload
     */
    Class<? extends T> payloadType();

    /**
     * An optional reference to the source or origin of this event. This could be
     * the publisher itself or another structured providing context for the event.
     *
     * @return the caller or source of this event, or {@code null} if not provided
     */
    Object caller();
}
