package org.jmouse.access.spi;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The honest default: what is being done is bound to the thread doing it.
 *
 * <p>A caller with no framework has exactly one truthful answer to "how long does a publication
 * last" — as long as the code that published it is running, on the thread it is running on. A web
 * application's request scope is that same answer with a request wrapped around it, and a game's
 * turn is that answer with a turn wrapped around it. So this is the default rather than a fallback,
 * and a product replaces it only when its unit of work outlives a stack frame.
 *
 * <p>⚠️ <strong>It does not follow work handed to another thread.</strong> An {@code @Async} call,
 * an executor, a reactive hop: the publication does not travel, so a rule about the action does not
 * hold there. That is the safe direction for a conditional allow and the unsafe one for a conditional
 * deny — a deny that stops applying is a call that goes through. Anything dispatching authorization
 * onto another thread has to republish on it, and the shortest way to say that is that a window is a
 * {@code try}-with-resources: if it is not lexically around the check, it is not around the check.
 *
 * <p>⚠️ <strong>Closing out of order is a defect, not a recoverable state.</strong> The handle
 * restores the exact frame it covered and refuses if something else has been pushed and not popped in
 * between — silently unwinding somebody else's publication would leave a rule reading an action that
 * belongs to a call which has already returned.
 */
public class ThreadBoundAccessContextScope implements AccessContextScope {

    private final ThreadLocal<Deque<Published>> stack = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * ⚠️ <strong>A publication with no action of its own keeps the one it covered.</strong>
     *
     * <p>Not tidiness — the alternative is an open door. A caller that publishes only values (a class
     * stating the tenant once, an ambient contribution) would otherwise cover an outer
     * {@code entry.listByPurpose} with null, and every {@code deny when action == 'entry.listByPurpose'}
     * inside it would stop applying. A deny that stops applying is a call that goes through.
     *
     * <p>An action is a statement about <em>what is being done</em>, and a frame that says nothing
     * about that has not changed it.
     */
    @Override
    public AutoCloseable push(String action, Map<String, Object> values) {
        Deque<Published> frames = stack.get();
        Published        published = new Published(
                action == null ? action() : action, withoutNulls(values));

        frames.push(published);

        return () -> pop(published);
    }

    /**
     * ⚠️ Copied null-tolerantly, because {@link Map#copyOf} is not.
     *
     * <p>The values arrive from an {@link AccessContextScope} caller and, through
     * {@code AmbientAccessValues}, from a product's own code. One null value there would otherwise be
     * a {@link NullPointerException} on every guarded call — an authorization mechanism taking the
     * product down, which is the one thing it must never do. A name mapped to null and a name that is
     * absent mean the same thing to every rule, so dropping it loses nothing.
     */
    private static Map<String, Object> withoutNulls(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> copied = new LinkedHashMap<>(values.size());

        values.forEach((name, value) -> {
            if (name != null && value != null) {
                copied.put(name, value);
            }
        });

        return Collections.unmodifiableMap(copied);
    }

    @Override
    public String action() {
        Published current = stack.get().peek();
        return current == null ? null : current.action();
    }

    @Override
    public Map<String, Object> values() {
        Published current = stack.get().peek();
        return current == null ? Map.of() : current.values();
    }

    /**
     * Restores what was published before {@code expected}, and clears the thread once nothing is left.
     *
     * <p>The clearing is not tidiness. A pooled thread that keeps an empty deque keeps a strong
     * reference to whatever the map held for as long as the pool lives, and a leak of the values an
     * authorization decision was made with is the least pleasant kind.
     */
    private void pop(Published expected) {
        Deque<Published> frames = stack.get();
        Published        top    = frames.peek();

        if (top != expected) {
            throw new IllegalStateException(
                    "An access-context window is being closed out of order. Something published "
                    + "inside this one and did not close, so restoring now would hand the next check "
                    + "an action belonging to a call that has already returned. Publish with "
                    + "try-with-resources.");
        }

        frames.pop();

        if (frames.isEmpty()) {
            stack.remove();
        }
    }

    private record Published(String action, Map<String, Object> values) {
    }
}
