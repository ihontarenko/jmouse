package org.jmouse.core.events;

/**
 * Represents a logical category of events.
 *
 * <p>Categories are used for filtering, routing, and metrics aggregation.</p>
 */
@FunctionalInterface
public interface EventCategory {

    EventCategory UNCATEGORIZED = () -> "UNCATEGORIZED";

    /**
     * @return stable category identifier (e.g. "context", "bean.lookup")
     */
    String id();

    /**
     * @return human-readable category label
     */
    default String label() {
        return id();
    }
}
