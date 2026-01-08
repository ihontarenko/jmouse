package org.jmouse.core.events;

/**
 * A handle representing an active subscription.
 *
 * <p>Closing the subscription removes the listener from the manager.</p>
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {

    /**
     * Cancel this subscription.
     */
    @Override
    void close();
}
