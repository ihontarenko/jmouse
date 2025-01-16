package svit.web.context;

import svit.observer.AbstractEvent;

/**
 * An event class for publishing {@link ApplicationContext} updates or lifecycle changes.
 * This event carries an {@link ApplicationContext} as its payload and provides constants
 * indicating different stages in the application's lifecycle. Typical usage involves
 * creating and firing the event at key application milestones, allowing interested
 * observers to respond accordingly.
 *
 * <p>Event name constants:
 * <ul>
 *   <li>{@link #EVENT_BEFORE_CONTEXT_REFRESH}: Fired before the application context is refreshed.</li>
 *   <li>{@link #EVENT_AFTER_CONTEXT_REFRESH}: Fired after the application context is successfully refreshed.</li>
 * </ul>
 *
 * @see ApplicationContext
 * @see AbstractEvent
 */
public class ApplicationContextEvent extends AbstractEvent<ApplicationContext> {

    /**
     * Constant for the "before context refresh" event name.
     */
    public static final String EVENT_BEFORE_CONTEXT_REFRESH = "BEFORE_CONTEXT_REFRESH";

    /**
     * Constant for the "after context refresh" event name.
     */
    public static final String EVENT_AFTER_CONTEXT_REFRESH  = "AFTER_CONTEXT_REFRESH";

    /**
     * Constructs a new {@code ApplicationContextEvent} with the specified name,
     * {@link ApplicationContext} payload, and optional caller reference.
     *
     * @param name    the name identifying this event (e.g., one of the provided constants)
     * @param payload the {@link ApplicationContext} payload relevant to this event
     * @param caller  an optional reference to the source or origin of this event
     */
    public ApplicationContextEvent(String name, ApplicationContext payload, Object caller) {
        super(name, payload, caller);
    }
}