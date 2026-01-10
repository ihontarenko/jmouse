package org.jmouse.core.events;

import org.jmouse.core.context.execution.ExecutionContext;
import org.jmouse.core.context.execution.ExecutionContextHolder;
import org.jmouse.core.trace.TraceContext;
import org.jmouse.core.trace.TraceKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.jmouse.core.Verify.nonNull;

/**
 * Thread-safe event bus that supports per-event and global subscriptions.
 *
 * <p>
 * {@code EventManager} manages listener registration and dispatches published events
 * to listeners subscribed to a specific {@link EventName} and to global listeners
 * registered under {@link #ALL_EVENTS}.
 * </p>
 *
 * <h3>Key features</h3>
 * <ul>
 *   <li>Thread-safe subscription and publishing via {@link ConcurrentHashMap} and {@link CopyOnWriteArrayList}</li>
 *   <li>Subscription handle ({@link Subscription}) for easy unsubscription</li>
 *   <li>Listener error isolation via {@link EventDispatchErrorHandler}</li>
 *   <li>Optional trace enrichment for non-traceable events</li>
 * </ul>
 *
 * <h3>Thread-safety</h3>
 * <ul>
 *   <li>Subscription/unsubscription is safe concurrently with publishing.</li>
 *   <li>Publishing iterates over snapshot-style lists ({@link CopyOnWriteArrayList}).</li>
 * </ul>
 */
public final class EventManager {

    /**
     * Special event name used for listeners that should receive all published events.
     */
    public static final EventName ALL_EVENTS = new AnyEvent(EventCategory.UNCATEGORIZED);

    /**
     * Internal logger for subscription and dispatch diagnostics.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

    /**
     * Listener registry grouped by {@link EventName}.
     */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<EventListener<?>>> listeners
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Subscription>> subscriptions
            = new ConcurrentHashMap<>();

    /**
     * Handler invoked when a listener fails during dispatch.
     * <p>
     * Marked {@code volatile} to allow runtime replacement with visibility across threads.
     */
    private volatile EventDispatchErrorHandler errorHandler = EventManager::defaultErrorHandler;

    /**
     * Default listener error handler that logs failures and isolates them from the publisher.
     *
     * @param event    the event being dispatched (may be {@code null} in defensive scenarios)
     * @param listener the listener that failed (may be {@code null} in defensive scenarios)
     * @param error    the failure raised by the listener
     */
    private static void defaultErrorHandler(Event<?> event, EventListener<?> listener, Throwable error) {
        LOGGER.error(
                "Listener '{}' failed for event '{}': {}",
                (listener == null ? "<unknown>" : listener.name()),
                (event == null ? "<unknown>" : event.name()),
                error.toString(), error
        );
    }

    /**
     * Subscribe a listener to a specific event name.
     *
     * <p>
     * The returned {@link Subscription} can be used to unsubscribe the listener
     * from the same event name.
     * </p>
     *
     * @param name     event name to subscribe to
     * @param listener listener instance
     * @return subscription handle
     */
    public Subscription subscribe(EventName name, EventListener<?> listener) {
        nonNull(name, "event-name");
        nonNull(listener, "listener");

        listeners.computeIfAbsent(name.id(), k -> new CopyOnWriteArrayList<>()).add(listener);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Subscribed '{}' to '{}'", listener.name(), name);
        }

        return () -> unsubscribe(name, listener);
    }

    /**
     * Subscribe a listener to all events.
     * <p>
     * This is a convenience shortcut for subscribing to {@link #ALL_EVENTS}.
     * </p>
     *
     * @param listener listener instance
     * @return subscription handle
     */
    public Subscription subscribeAll(EventListener<?> listener) {
        return subscribe(ALL_EVENTS, listener);
    }

    /**
     * Unsubscribe a listener from a specific event name.
     *
     * @param name     event name
     * @param listener listener instance
     * @return {@code true} if the listener was removed, {@code false} otherwise
     */
    public boolean unsubscribe(EventName name, EventListener<?> listener) {
        nonNull(listener, "listener");
        nonNull(name, "event-name");

        var arrayList = listeners.get(name.id());

        if (arrayList == null) {
            return false;
        }

        boolean removed = arrayList.remove(listener);

        if (arrayList.isEmpty()) {
            listeners.remove(name.id(), arrayList);
        }

        if (removed && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unsubscribed '{}' from '{}'", listener.name(), name);
        }

        return removed;
    }

    /**
     * Remove all listeners for all event names, including {@link #ALL_EVENTS}.
     */
    public void clearAll() {
        listeners.clear();
    }

    /**
     * Publish an event to all matching listeners.
     * <p>
     * The event is dispatched to:
     * </p>
     * <ul>
     *   <li>listeners subscribed to {@code event.name()}</li>
     *   <li>listeners subscribed to {@link #ALL_EVENTS}</li>
     * </ul>
     *
     * <p>
     * Listener failures are isolated and routed to the configured
     * {@link #setErrorHandler(EventDispatchErrorHandler) error handler}.
     * </p>
     *
     * <p>
     * If the given event is not {@link TraceableEvent traceable}, it is wrapped
     * with trace metadata (see {@link #toTraceableEvent(Event)}).
     * </p>
     *
     * @param event event to publish
     * @param <T>   payload type
     */
    public <T> void publish(Event<T> event) {
        nonNull(event, "event");

        Event<T> effective = toTraceableEvent(event);

        dispatch(effective, listeners.get(effective.name().id()));
        dispatch(effective, listeners.get(ALL_EVENTS.id()));
    }

    /**
     * Returns the set of currently registered event names.
     * <p>
     * The returned set is a live view of the internal registry.
     * </p>
     *
     * @return registered event names
     */
    public Set<String> eventNames() {
        return listeners.keySet();
    }

    /**
     * Remove all listeners subscribed to the given event name.
     *
     * @param name event name
     */
    public void clear(EventName name) {
        nonNull(name, "event-name");
        listeners.remove(name);
    }

    /**
     * Returns the number of listeners registered for a given event name.
     *
     * @param name event name
     * @return listener count
     */
    public int listenerCount(EventName name) {
        nonNull(name, "event-name");
        List<EventListener<?>> list = listeners.get(name);
        return list == null ? 0 : list.size();
    }

    /**
     * Configure the dispatch error handler.
     * <p>
     * The handler is invoked when:
     * </p>
     * <ul>
     *   <li>{@link EventListener#supportsPayloadType(Class)} throws</li>
     *   <li>{@link EventListener#onEvent(Event)} throws</li>
     * </ul>
     *
     * @param errorHandler handler invoked on listener failures
     */
    public void setErrorHandler(EventDispatchErrorHandler errorHandler) {
        this.errorHandler = nonNull(errorHandler, "errorHandler");
    }

    /**
     * Dispatch an event to a bucket of listeners.
     * <p>
     * Each listener is checked for payload compatibility via
     * {@link EventListener#supportsPayloadType(Class)} before invocation.
     * </p>
     *
     * @param event   event to dispatch
     * @param bucket  listener bucket (may be {@code null})
     * @param <T>     payload type
     */
    private <T> void dispatch(Event<T> event, List<EventListener<?>> bucket) {
        if (bucket == null || bucket.isEmpty()) {
            return;
        }

        Class<?>  payloadType = event.payloadType();
        EventName eventName   = event.name();

        for (EventListener<?> listener : bucket) {
            @SuppressWarnings("unchecked") EventListener<T> eventListener = (EventListener<T>) listener;
            boolean supported;

            try {
                supported = eventListener.supportsPayloadType(payloadType);
            } catch (Throwable exception) {
                errorHandler.onListenerError(event, listener, exception);
                continue;
            }

            if (!supported) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Skip event '{}({})' for '{}'", event.name(), payloadType.getName(), listener.name());
                }
                continue;
            }

            try {
                if (LOGGER.isDebugEnabled()) {
                    TraceContext trace = null;

                    if (event instanceof TraceableEvent<?> traceable) {
                        trace = traceable.trace();
                    }

                    LOGGER.debug(
                            "{} {} :: {} → {}",
                            trace != null ? formatTraceMessage(trace) : "[no-trace]",
                            eventName.category().label(),
                            eventName.label(),
                            listener.name()
                    );
                }
                eventListener.onEvent(event);
            } catch (Throwable e) {
                errorHandler.onListenerError(event, listener, e);
            }
        }
    }

    private static String formatTraceMessage(TraceContext t) {
        return "[%s :: %s :: %s :: %d]"
                .formatted(
                        t.correlationId(),
                        t.spanId(),
                        t.parentSpanId() != null ? t.parentSpanId() : "-",
                        t.depth()
                );
    }

    private <T> Event<T> toTraceableEvent(Event<T> event) {
        if (event instanceof TraceableEvent<?>) {
            return event;
        }

        ExecutionContext context = ExecutionContextHolder.current();
        TraceContext     trace   = context.get(TraceKeys.TRACE);

        if (trace == null) {
            trace = TraceContext.root();
        } else {
            trace = trace.touch();
        }

        return new TracedEvent<>(event, trace);
    }
}
