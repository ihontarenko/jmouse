package org.jmouse.crawler.api;

/**
 * Decision logging interface used by the crawler runtime to record
 * accept/reject outcomes of scheduling or processing decisions. 🧾
 *
 * <p>{@code DecisionLog} is intentionally minimal and side-effect–oriented.
 * It allows the runtime to emit structured decision events without
 * coupling to a specific logging, tracing, or metrics backend.</p>
 *
 * <p>Typical use cases:</p>
 * <ul>
 *   <li>recording why a {@link ProcessingTask} was accepted for execution</li>
 *   <li>recording why a task was rejected, deferred, or dropped</li>
 *   <li>feeding audit logs, metrics, or debug traces</li>
 * </ul>
 *
 * <p>The {@code code} parameter is intended to be machine-readable
 * (stable identifier), while {@code message} is intended for
 * human-readable diagnostics.</p>
 */
public interface DecisionLog {

    /**
     * Record an accepted decision.
     *
     * <p>This method should be called when a task or action is allowed
     * to proceed (e.g. scheduled, executed, retried).</p>
     *
     * @param code    symbolic decision code (e.g. {@code "task.ready"}, {@code "retry.allowed"})
     * @param message human-readable explanation of the decision
     */
    void accept(String code, String message);

    /**
     * Record a rejected decision.
     *
     * <p>This method should be called when a task or action is denied,
     * deferred, or discarded (e.g. politeness delay, max retries exceeded).</p>
     *
     * @param code    symbolic decision code (e.g. {@code "politeness.defer"}, {@code "retry.exhausted"})
     * @param message human-readable explanation of the decision
     */
    void reject(String code, String message);
}
