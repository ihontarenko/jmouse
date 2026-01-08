package org.jmouse.beans;

import org.jmouse.beans.events.BeanContextEventSupport;
import org.jmouse.core.Priority;
import org.jmouse.core.events.EventCategory;
import org.jmouse.core.events.EventListener;
import org.jmouse.core.events.EventName;
import org.jmouse.core.events.annotation.Listener;
import org.jmouse.core.reflection.Reflections;

/**
 * 🔌 Bridges {@link EventListener} beans into the {@link org.jmouse.core.events.EventManager}
 * owned by a {@link BeanContext} that supports {@link BeanContextEventSupport}.
 * <p>
 * This initializer scans the context for beans implementing {@link EventListener} and,
 * if their class is annotated with {@link Listener}, subscribes them to the declared
 * event names.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Runs only when {@code context} implements {@link BeanContextEventSupport}.</li>
 *   <li>Discovers all beans assignable to {@link EventListener}.</li>
 *   <li>Reads {@link Listener#events()} from the listener class.</li>
 *   <li>Subscribes the listener instance to each declared event name.</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>If {@link Listener#events()} is {@code null}, the listener is ignored.</li>
 *   <li>Subscription is performed against {@link BeanContextEventSupport#getEventManager()}.</li>
 * </ul>
 */
@Priority(Integer.MAX_VALUE)
public final class EventBridgeContextInitializer implements BeanContextInitializer {

    /**
     * Initializes event bridging for the given {@link BeanContext}.
     * <p>
     * If the context exposes an event system via {@link BeanContextEventSupport}, this method
     * registers all {@link EventListener} beans annotated with {@link Listener} into
     * the context event manager.
     *
     * @param context the bean context to initialize
     */
    @Override
    public void initialize(BeanContext context) {
        for (String name : context.getBeanNames(EventListener.class)) {
            EventListener<?> listener = context.getBean(name);

            String[] eventNames = Reflections.getAnnotationValue(listener.getClass(), Listener.class, Listener::events);

            if (eventNames != null) {
                for (String eventName : eventNames) {
                    context.getEventManager().subscribe(EventName.of(eventName, EventCategory.UNCATEGORIZED), listener);
                }
            }
        }
    }

}
