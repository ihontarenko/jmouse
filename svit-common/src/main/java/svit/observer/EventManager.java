package svit.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A manager for handling event subscriptions and notifications.
 * It allows subscribers to listen for specific event types and notifies them when those events occur.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Define custom event and listener implementations
 * public class MyEvent implements Event<String> {
 *     private final String name;
 *     private final String payload;
 *
 *     public MyEvent(String name, String payload) {
 *         this.name = name;
 *         this.payload = payload;
 *     }
 *
 *     @Override
 *     public String name() {
 *         return name;
 *     }
 *
 *     @Override
 *     public String payload() {
 *         return payload;
 *     }
 * }
 *
 * public class MyEventListener implements EventListener<String> {
 *     @Override
 *     public void update(Event<? super String> event) {
 *         System.out.println("Received event: " + event.name() + " with payload: " + event.payload());
 *     }
 * }
 *
 * // Create and use EventManager
 * public static void main(String[] args) {
 *     EventManager eventManager = new EventManager("event1", "event2");
 *
 *     MyEventListener listener = new MyEventListener();
 *     eventManager.subscribe("event1", listener);
 *
 *     MyEvent event = new MyEvent("event1", "Hello, World!");
 *     eventManager.notify(event);
 * }
 * }</pre>
 */
final public class EventManager {

    /**
     * A logger instance
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

    /**
     * A singleton instance of manager with scanned annotated listeners
     */
    public static final EventManager INSTANCE = EventManagerFactory.create();

    /**
     * A map that associates event types with lists of subscribed listeners.
     */
    private final Map<String, List<EventListener<?>>> listeners = new HashMap<>();

    /**
     * Subscribes a listener to a specific event type.
     *
     * @param eventType the type of event to subscribe to.
     * @param listener  the listener to be notified when the event occurs.
     */
    public void subscribe(String eventType, EventListener<?> listener) {
        LOGGER.info("SUBSCRIBE NEW LISTENER '{}' FOR EVENT '{}'", listener.name(), eventType);
        listeners.computeIfAbsent(eventType, key -> new ArrayList<>()).add(listener);
    }

    /**
     * Unsubscribes a listener from a specific event type.
     *
     * @param eventType the type of event to unsubscribe from.
     * @param listener  the listener to be removed.
     */
    public void unsubscribe(String eventType, EventListener<?> listener) {
        List<EventListener<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            if (eventListeners.isEmpty()) {
                unsubscribe(eventType);
            }
        }
    }

    /**
     * Removes all listeners subscribed to a specific event type.
     *
     * @param eventType the type of event to clear all subscriptions for.
     */
    public void unsubscribe(String eventType) {
        listeners.remove(eventType);
    }

    /**
     * Notifies all listeners subscribed to the specified event type.
     *
     * @param event the event to be dispatched to listeners.
     * @param <T>   the type of the event payload.
     */
    @SuppressWarnings("unchecked")
    public <T> void notify(Event<T> event) {
        List<EventListener<?>> eventListeners = listeners.get(event.name());
        if (eventListeners != null) {
            for (EventListener<?> listener : eventListeners) {
                if (listener.supports(event.payloadType())) {
                    EventListener<T> typedListener = (EventListener<T>) listener;
                    LOGGER.info("FIRE EVENT '{}' FOR '{}'", event.name(), listener.name());
                    typedListener.update(event);
                } else {
                    LOGGER.info("SKIP EVENT '{}({})' FOR '{}'", event.name(), event.payloadType(), listener.name());
                }
            }
        }
    }
}

