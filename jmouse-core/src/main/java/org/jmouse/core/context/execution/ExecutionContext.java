package org.jmouse.core.context.execution;

import org.jmouse.core.context.ContextKey;

import java.util.Map;

/**
 * 🧩 Immutable execution context abstraction.
 *
 * <p>
 * {@code ExecutionContext} represents a typed, immutable key–value container
 * that carries execution-scoped metadata (e.g. request IDs, user info,
 * diagnostic flags) through a call chain.
 * </p>
 *
 * <h3>Design principles</h3>
 * <ul>
 *   <li><b>Immutability</b> – updates produce a new context instance</li>
 *   <li><b>Type safety</b> – values are accessed via typed {@link ContextKey}</li>
 *   <li><b>Thread-bound usage</b> – typically managed by {@link ExecutionContextHolder}</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * ExecutionContext ctx = new DefaultExecutionContext()
 *         .with(ContextKeys.REQUEST_ID, "req-123")
 *         .with(ContextKeys.USER_ID, "u-42");
 *
 * try (var scope = ExecutionContextHolder.open(ctx)) {
 *     String requestId = ExecutionContextHolder.current()
 *             .get(ContextKeys.REQUEST_ID);
 * }
 * }</pre>
 *
 * <h3>Async propagation</h3>
 * <pre>{@code
 * ExecutionSnapshot snapshot = ExecutionContextHolder.capture();
 *
 * executor.submit(() -> {
 *     try (var scope = ExecutionContextHolder.open(snapshot)) {
 *         service.handle();
 *     }
 * });
 * }</pre>
 */
public interface ExecutionContext {

    /**
     * Returns the value associated with the given context key.
     *
     * @param key typed context key
     * @param <T> value type
     * @return the associated value, or {@code null} if not present
     */
    <T> T get(ContextKey<T> key);

    /**
     * Returns a new execution context with the given key–value pair applied.
     * <p>
     * The original context instance remains unchanged.
     * </p>
     *
     * @param key   typed context key
     * @param value value to associate (may be {@code null})
     * @param <T>   value type
     * @return a new {@link ExecutionContext} instance containing the update
     */
    <T> ExecutionContext with(ContextKey<T> key, T value);

    /**
     * Returns all entries contained in this context.
     * <p>
     * The returned map represents a snapshot view of the context state
     * and should be treated as read-only.
     * </p>
     *
     * @return map of context entries
     */
    Map<ContextKey<?>, Object> entries();

    /**
     * Creates an immutable snapshot of this execution context.
     * <p>
     * Snapshots are primarily used to propagate context across
     * asynchronous execution boundaries.
     * </p>
     *
     * @return snapshot of this execution context
     */
    ExecutionSnapshot snapshot();
}
