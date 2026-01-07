package org.jmouse.core.events;

import org.jmouse.core.Verify;
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
     * A map that associates event types with lists of subscribed listeners.
     */
    private final Map<String, List<EventListener<?>>> listeners = new HashMap<>();

    /**
     * Subscribes a listener to a specific event type.
     *
     * @param eventName the type of event to subscribe to.
     * @param listener  the listener to be notified when the event occurs.
     */
    public void subscribe(String eventName, EventListener<?> listener) {
        Verify.nonNull(eventName, "event-name");
        Verify.nonNull(listener, "listener");

        LOGGER.info("Subscribe listener '{}' to event '{}'", listener.name(), eventName);

        listeners.computeIfAbsent(eventName, key -> new ArrayList<>()).add(listener);
    }

    /**
     * Unsubscribes a listener from a specific event type.
     *
     * @param eventName the type of event to unsubscribe from.
     * @param listener  the listener to be removed.
     */
    public void unsubscribe(String eventName, EventListener<?> listener) {
        Verify.nonNull(eventName, "event-name");
        Verify.nonNull(listener, "listener");

        List<EventListener<?>> eventListeners = listeners.get(eventName);
        if (eventListeners == null) {
            return;
        }

        eventListeners.remove(listener);
        if (eventListeners.isEmpty()) {
            listeners.remove(eventName);
        }
    }

    /**
     * Removes all listeners subscribed to a specific event type.
     *
     * @param eventName the type of event to clear all subscriptions for.
     */
    public void unsubscribe(String eventName) {
        Verify.nonNull(eventName, "event-name");
        listeners.remove(eventName);
    }

    /**
     * Notifies all listeners subscribed to the specified event type.
     *
     * @param event the event to be dispatched to listeners.
     * @param <T>   the type of the event payload.
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(Event<T> event) {
        Verify.nonNull(event, "event");

        List<EventListener<?>> listeners   = this.listeners.get(event.name());
        Class<?>               payloadType = event.payloadType();

        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        for (EventListener<?> rawListener : listeners) {
            EventListener<T> listener = (EventListener<T>) rawListener;

            if (listener.supportsPayloadType(payloadType)) {
                LOGGER.info("Dispatch event '{}' to '{}'", event.name(), listener.name());
                listener.onEvent(event);
            } else {
                LOGGER.debug("Skip event '{}({})' for '{}'", event.name(), payloadType.getName(), listener.name());
            }
        }
    }
}

