package org.jmouse.core.events;

import org.jmouse.core.trace.TraceContext;

/**
 * Marker interface for events enriched with {@link TraceContext}.
 */
public interface TraceableEvent<T> extends Event<T> {

    /**
     * @return trace metadata
     */
    TraceContext trace();
}
