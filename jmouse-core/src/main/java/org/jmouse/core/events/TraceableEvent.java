package org.jmouse.core.events;

/**
 * Marker interface for events enriched with {@link EventTrace}.
 */
public interface TraceableEvent<T> extends Event<T> {

    /**
     * @return trace metadata
     */
    EventTrace trace();
}
