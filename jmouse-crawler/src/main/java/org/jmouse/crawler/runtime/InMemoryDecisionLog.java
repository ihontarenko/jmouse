package org.jmouse.crawler.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory implementation of {@link DecisionLog}. 🧾
 *
 * <p>{@code InMemoryDecisionLog} collects decision events in a simple list.
 * It is primarily intended for:</p>
 * <ul>
 *   <li>testing and assertions</li>
 *   <li>debugging scheduler and runtime behavior</li>
 *   <li>small, short-lived crawls where decision history is bounded</li>
 * </ul>
 *
 * <p>⚠️ This implementation is <strong>not thread-safe</strong>.
 * It is expected to be used in single-threaded runners or test scenarios.
 * For concurrent runtimes, a synchronized or lock-free implementation
 * should be provided.</p>
 */
public final class InMemoryDecisionLog implements DecisionLog {

    /**
     * Single decision entry recorded by the log.
     *
     * @param accepted {@code true} if the decision was accepted; {@code false} if rejected
     * @param code     machine-readable decision code
     * @param message  human-readable explanation
     */
    public record Entry(boolean accepted, String code, String message) {}

    private final List<Entry> entries = new ArrayList<>();

    /**
     * Record an accepted decision.
     *
     * @param code    symbolic decision code
     * @param message human-readable explanation
     */
    @Override
    public void accept(String code, String message) {
        entries.add(new Entry(true, code, message));
    }

    /**
     * Record a rejected decision.
     *
     * @param code    symbolic decision code
     * @param message human-readable explanation
     */
    @Override
    public void reject(String code, String message) {
        entries.add(new Entry(false, code, message));
    }

    /**
     * Return an immutable view of all recorded decision entries.
     *
     * <p>The returned list reflects the current state of the log
     * but cannot be modified by the caller.</p>
     *
     * @return unmodifiable list of decision entries
     */
    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Return a concise string representation for diagnostics.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [entries=" + entries.size() + "]";
    }
}
