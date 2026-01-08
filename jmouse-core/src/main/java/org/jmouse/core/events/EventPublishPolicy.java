package org.jmouse.core.events;

import org.jmouse.core.trace.TraceContext;

/**
 * 📨 Strategy interface that decides whether an event should be published.
 * <p>
 * {@code EventPublishPolicy} is consulted by the event publisher
 * before dispatching an event to listeners.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Filter events by name, origin, or execution context</li>
 *   <li>Support lightweight runtime configuration</li>
 *   <li>Avoid unnecessary event dispatch overhead</li>
 * </ul>
 *
 * <p>This interface is intentionally {@link FunctionalInterface}
 * to allow inline and lambda-based policies.</p>
 */
@FunctionalInterface
public interface EventPublishPolicy {

    /**
     * Determines whether the given event should be published.
     *
     * @param eventName the stable event name
     * @param trace     optional event trace metadata (may be {@code null})
     * @param caller    the publishing component or context
     * @return {@code true} to publish the event, {@code false} to skip it
     */
    boolean shouldPublish(EventName eventName, TraceContext trace, Object caller);

    /**
     * Publish all events without filtering.
     *
     * @return a policy that always publishes events
     */
    static EventPublishPolicy publishAll() {
        return (n, t, c) -> true;
    }

    /**
     * Publish only root-level events.
     * <p>
     * Events with no trace information or with a trace depth of {@code 0}
     * are considered root events.
     *
     * @return a policy that publishes only root-level events
     */
    static EventPublishPolicy rootOnly() {
        return (n, t, c) -> t == null || t.depth() == 0;
    }

}
