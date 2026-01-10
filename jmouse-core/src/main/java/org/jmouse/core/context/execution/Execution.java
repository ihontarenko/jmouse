package org.jmouse.core.context.execution;

import org.jmouse.core.context.ContextKey;

import java.util.function.Supplier;

/**
 * 🎯 Convenience utility for executing code within a temporary execution context.
 *
 * <p>
 * {@code Execution} provides a concise, functional-style API for running
 * a {@link Runnable} or {@link Supplier} with a single context key overridden
 * for the duration of execution.
 * </p>
 *
 * <h3>Purpose</h3>
 * <ul>
 *   <li>Avoid manual {@link ExecutionContextHolder#open(ExecutionContext)} boilerplate</li>
 *   <li>Encapsulate common "with one extra key" execution pattern</li>
 *   <li>Encourage scoped, short-lived context modifications</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 *
 * <h4>1) Execute with a temporary context value</h4>
 * <pre>{@code
 * Execution.in(ContextKeys.USER_ID, "u-42", () -> {
 *     service.handle();
 * });
 * }</pre>
 *
 * <h4>2) Execute and return a value</h4>
 * <pre>{@code
 * String tenant = Execution.in(ContextKeys.TENANT, "tenant-A", () -> {
 *     return repository.currentTenant();
 * });
 * }</pre>
 *
 * <h4>3) Nested usage</h4>
 * <pre>{@code
 * Execution.in(ContextKeys.REQUEST_ID, "req-123", () -> {
 *     Execution.in(ContextKeys.DEBUG, true, () -> {
 *         logger.debug("debug enabled");
 *     });
 * });
 * }</pre>
 *
 * <h3>Semantics</h3>
 * <ul>
 *   <li>The original execution context is always restored after execution</li>
 *   <li>The provided key is removed if {@code value} is {@code null}</li>
 *   <li>Nested invocations compose naturally via the context stack</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
public final class Execution {

    private Execution() {}

    /**
     * Executes the given action with the specified context key temporarily applied.
     *
     * <p>
     * A new {@link ExecutionContext} is derived from the current context
     * by applying the given key–value pair, then installed for the duration
     * of the supplied action.
     * </p>
     *
     * @param key    context key to apply
     * @param value  value to associate with the key (may be {@code null})
     * @param action action to execute
     * @param <T>    return type
     * @return the value returned by the action
     */
    public static <T> T in(ContextKey<?> key, Object value, Supplier<T> action) {
        ExecutionContext current = ExecutionContextHolder.current();
        ExecutionContext next    =
                current.with((ContextKey<Object>) key, value);

        try (ExecutionContextHolder.Scope ignored =
                     ExecutionContextHolder.open(next)) {
            return action.get();
        }
    }

    /**
     * Executes the given runnable with the specified context key temporarily applied.
     *
     * <p>
     * This is a convenience overload for {@link #in(ContextKey, Object, Supplier)}
     * when no return value is required.
     * </p>
     *
     * @param key    context key to apply
     * @param value  value to associate with the key (may be {@code null})
     * @param action runnable to execute
     */
    public static void in(ContextKey<?> key, Object value, Runnable action) {
        in(key, value, () -> {
            action.run();
            return null;
        });
    }
}
