package org.jmouse.core.events;

/**
 * Handles errors thrown by listeners during event dispatch.
 */
@FunctionalInterface
public interface EventDispatchErrorHandler {

    /**
     * Called when a listener throws an exception during dispatch.
     *
     * @param event     the event being dispatched
     * @param listener  the listener that failed
     * @param error     thrown error
     */
    void onListenerError(Event<?> event, EventListener<?> listener, Throwable error);
}
