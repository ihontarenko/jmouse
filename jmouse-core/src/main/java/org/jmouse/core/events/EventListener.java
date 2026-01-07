package org.jmouse.core.events;

/**
 * Listener for events published by {@link EventManager}.
 *
 * @param <T> payload type
 */
public interface EventListener<T> {

    /**
     * Logical listener name (used for diagnostics/logging).
     *
     * @return listener name
     */
    String name();

    /**
     * Handle an incoming event.
     *
     * @param event event instance
     */
    void onEvent(Event<T> event);

    /**
     * Declared payload base type this listener is designed for.
     * Used as a coarse filter and for documentation purposes.
     *
     * @return payload base type
     */
    Class<?> payloadType();

    /**
     * Decide whether this listener supports an actual payload type.
     *
     * @param payloadType actual payload class
     * @return true if supported
     */
    boolean supportsPayloadType(Class<?> payloadType);
}
