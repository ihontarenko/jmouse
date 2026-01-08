package org.jmouse.core.trace;

import org.jmouse.core.context.ExecutionContext;
import org.jmouse.core.context.ExecutionContextHolder;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trace-local deduplication storage backed by {@link ExecutionContext}.
 *
 * Stores a concurrent "seen keys" set in the current {@link ExecutionContext}.
 * The storage lives as long as the ExecutionContext scope lives.
 */
public final class TraceDeduplicate {

    /**
     * Key under which we store the "seen set" in ExecutionContext.
     * If your ExecutionContext keys are strongly typed, replace Object with your key type.
     */
    public static final Object SEEN_KEYS = new Object();

    private TraceDeduplicate() {}

    /**
     * Returns {@code true} if this is the first time the given key is seen in the current trace scope.
     * Returns {@code false} if the key was already registered before.
     */
    public static boolean firstTime(String key) {
        if (key == null || key.isBlank()) {
            // Defensive: treat empty key as non-deduplicable (always publish)
            return true;
        }

        ExecutionContext ctx = ExecutionContextHolder.current();
        if (ctx == null) {
            // No context -> cannot dedup safely, allow publish
            return true;
        }

        Set<String> seen = getOrCreateSeenSet(ctx);
        return seen.add(key);
    }

    /**
     * Clears trace-local dedup state in the current ExecutionContext.
     * Optional utility for tests or for explicit boundaries.
     */
    public static void clear() {
        ExecutionContext context = ExecutionContextHolder.current();
        if (context == null) {
            return;
        }

        // If your ExecutionContext supports remove, use it; otherwise overwrite with empty set.
        Set<String> empty = Collections.newSetFromMap(new ConcurrentHashMap<>());
        ExecutionContextHolder.open(context.with(SEEN_KEYS, empty)).close();
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getOrCreateSeenSet(ExecutionContext ctx) {
        Object value = ctx.get(SEEN_KEYS);

        if (value instanceof Set<?> existing) {
            return (Set<String>) existing;
        }

        Set<String> created = Collections.newSetFromMap(new ConcurrentHashMap<>());
        // Persist into the current context by opening a derived context.
        // This matches your pattern: ExecutionContext is immutable, so we open ctx.with(...)
        try (ExecutionContextHolder.Scope ignored = ExecutionContextHolder.open(ctx.with(SEEN_KEYS, created))) {
            // nothing
        }
        return created;
    }
}
