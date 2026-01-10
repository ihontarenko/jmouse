package org.jmouse.core.context.execution;

import org.jmouse.core.context.ContextKey;

import java.util.Map;

/**
 * 📦 Immutable snapshot of an {@link ExecutionContext}.
 *
 * <p>
 * {@code ExecutionSnapshot} captures a stable view of context entries
 * at a specific point in time. It is primarily used to propagate
 * execution context across asynchronous boundaries (e.g. threads,
 * executors, deferred tasks).
 * </p>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * ExecutionSnapshot snapshot = ExecutionContextHolder.capture();
 *
 * executor.submit(() -> {
 *     try (var scope = ExecutionContextHolder.open(snapshot)) {
 *         service.handle();
 *     }
 * });
 * }</pre>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>Snapshots are immutable and thread-safe.</li>
 *   <li>The snapshot does not hold references to thread-local state.</li>
 *   <li>Restoring a snapshot always creates a new {@link ExecutionContext} instance.</li>
 * </ul>
 *
 * @param entries key-value pairs captured from the execution context
 */
public record ExecutionSnapshot(
        Map<ContextKey<?>, Object> entries
) {
}
