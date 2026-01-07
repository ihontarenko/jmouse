package org.jmouse.beans;

import org.jmouse.beans.events.BeanContextEvents;
import org.jmouse.core.Priority;
import org.jmouse.core.observer.EventListener;
import org.jmouse.core.observer.annotation.Listener;
import org.jmouse.core.reflection.Reflections;

/**
 * 🔌 Bridges {@link EventListener} beans into the {@link org.jmouse.core.observer.EventManager}
 * owned by a {@link BeanContext} that supports {@link BeanContextEvents}.
 * <p>
 * This initializer scans the context for beans implementing {@link EventListener} and,
 * if their class is annotated with {@link Listener}, subscribes them to the declared
 * event names.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Runs only when {@code context} implements {@link BeanContextEvents}.</li>
 *   <li>Discovers all beans assignable to {@link EventListener}.</li>
 *   <li>Reads {@link Listener#events()} from the listener class.</li>
 *   <li>Subscribes the listener instance to each declared event name.</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>If {@link Listener#events()} is {@code null}, the listener is ignored.</li>
 *   <li>Subscription is performed against {@link BeanContextEvents#getEventManager()}.</li>
 * </ul>
 */
@Priority(Integer.MAX_VALUE)
public final class EventBridgeContextInitializer implements BeanContextInitializer {

    /**
     * Initializes event bridging for the given {@link BeanContext}.
     * <p>
     * If the context exposes an event system via {@link BeanContextEvents}, this method
     * registers all {@link EventListener} beans annotated with {@link Listener} into
     * the context event manager.
     *
     * @param context the bean context to initialize
     */
    @Override
    public void initialize(BeanContext context) {
        if (context instanceof BeanContextEvents events) {
            for (String name : context.getBeanNames(EventListener.class)) {
                EventListener<?> listener = context.getBean(name);

                String[] eventNames = Reflections.getAnnotationValue(
                        listener.getClass(), Listener.class, Listener::events);

                if (eventNames != null) {
                    for (String eventName : eventNames) {
                        events.getEventManager().subscribe(eventName, listener);
                    }
                }
            }
        }
    }

}
