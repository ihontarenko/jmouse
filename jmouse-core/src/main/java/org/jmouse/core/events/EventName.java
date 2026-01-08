package org.jmouse.core.events;

import java.util.Objects;

/**
 * Represents a stable event identifier.
 *
 * <p>
 * Event names may be backed by enums or any other implementation.
 * The {@link #id()} must be stable across releases to avoid breaking external listeners.
 * </p>
 */
public interface EventName {

    /**
     * @return stable event identifier (e.g. "bean.lookup.started")
     */
    String id();

    /**
     * @return event category
     */
    EventCategory category();

    /**
     * @return human-readable label
     */
    default String label() {
        return id();
    }

    /**
     * @return true if this event represents a failure outcome
     */
    default boolean isFailure() {
        return id().endsWith(".failed") || id().endsWith(".error");
    }

    /**
     * Adapter for using plain strings as event names.
     */
    static EventName of(String id, EventCategory category) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        return new SimpleEventName(id, category);
    }
}
