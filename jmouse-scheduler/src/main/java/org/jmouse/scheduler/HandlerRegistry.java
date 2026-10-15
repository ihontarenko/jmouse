package org.jmouse.scheduler;

import org.jmouse.scheduler.spi.TriggerHandler;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The handlers this process knows, by key. Immutable once built; two handlers claiming one key are
 * refused at construction rather than resolved by order.
 */
public final class HandlerRegistry {

    private final KeyedRegistry<TriggerHandler> handlers;

    /**
     * @param handlers every handler; keys must be unique
     * @throws IllegalStateException when two handlers share a key
     */
    public HandlerRegistry(Collection<? extends TriggerHandler> handlers) {
        this.handlers = new KeyedRegistry<>(handlers, TriggerHandler::key, "handler");
    }

    /** @return a registry over the given handlers */
    public static HandlerRegistry of(TriggerHandler... handlers) {
        return new HandlerRegistry(List.of(handlers));
    }

    /** @return the handler registered under {@code key}, or empty */
    public Optional<TriggerHandler> find(String key) {
        return handlers.find(key);
    }

    /**
     * @return the handler registered under {@code key}
     * @throws IllegalArgumentException naming the key and the keys that do exist
     */
    public TriggerHandler require(String key) {
        return find(key).orElseThrow(() -> new IllegalArgumentException(
                "No trigger handler is registered under '" + key + "'; registered: " + keys() + "."));
    }

    /** Every registered key, sorted. */
    public Set<String> keys() {
        return handlers.keys();
    }
}
