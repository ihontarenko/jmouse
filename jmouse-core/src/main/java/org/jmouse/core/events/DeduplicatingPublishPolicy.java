package org.jmouse.core.events;

import org.jmouse.core.trace.TraceContext;
import org.jmouse.core.trace.TraceDeduplicate;

/**
 * Event publish policy that delegates to another policy and additionally deduplicates
 * events using a trace-local "seen keys" storage.
 */
public final class DeduplicatingPublishPolicy implements EventPublishPolicy {

    private final EventPublishPolicy     delegate;
    private final DeduplicateKeyStrategy strategy;

    public DeduplicatingPublishPolicy(EventPublishPolicy delegate, DeduplicateKeyStrategy strategy) {
        this.delegate = delegate;
        this.strategy = strategy;
    }

    @Override
    public boolean shouldPublish(EventName name, TraceContext trace, Object source) {
        if (!delegate.shouldPublish(name, trace, source)) {
            return false;
        }

        // Without trace, we cannot do correlation-aware deduplicate reliably -> allow publish.
        if (trace == null) {
            return true;
        }

        String key = strategy.keyOf(name, trace, source);
        // No key means "do not deduplicate" (always publish)
        if (key == null || key.isBlank()) {
            return true;
        }

        return TraceDeduplicate.firstTime(key);
    }

    /**
     * Generates deduplication keys for events.
     * Returning null/blank disables dedup for that event instance.
     */
    @FunctionalInterface
    public interface DeduplicateKeyStrategy {
        String keyOf(EventName name, TraceContext trace, Object source);
    }

}
