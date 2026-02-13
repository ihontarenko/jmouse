package org.jmouse.core.context;

/**
 * Thread-local scope holder for contextual values. 🧵
 *
 * <p>{@code ContextScope} allows temporarily binding a context object to the current thread
 * and restoring the previous value when the scope is closed.</p>
 *
 * <p>Typical usage pattern relies on try-with-resources to guarantee scope restoration:</p>
 * <pre>{@code
 * try (ContextScope.ScopeToken ignored = scope.open(context)) {
 *     // context is active in current thread
 * }
 * // previous context restored automatically
 * }</pre>
 *
 * <p><b>Thread-safety:</b> Context is isolated per-thread using {@link ThreadLocal}.</p>
 *
 * @param <T> context type
 */
public class ContextScope<T> {

    /**
     * Thread-local storage for the current context.
     */
    private static final ThreadLocal<Object> CURRENT = new ThreadLocal<>();

    /**
     * Open a new scope for the given context.
     *
     * <p>The provided context becomes current for the calling thread until the returned
     * {@link ScopeToken} is closed.</p>
     *
     * @param context new context value (may be {@code null})
     * @return scope token used to restore previous context
     */
    @SuppressWarnings("unchecked")
    public ScopeToken open(T context) {
        T previous = (T) CURRENT.get();

        CURRENT.set(context);

        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    /**
     * Return the current context bound to this thread.
     *
     * <p>If no context is currently bound, this method returns {@code null}.</p>
     *
     * <p><b>Type safety:</b> The returned value is cast to {@code T}. The caller must ensure
     * that the stored context matches the expected type.</p>
     *
     * @return current thread-bound context or {@code null} if none is set
     */
    @SuppressWarnings("unchecked")
    public T current() {
        return (T) CURRENT.get();
    }

    /**
     * Scope handle that restores the previous context when closed.
     *
     * <p>Designed for try-with-resources usage.</p>
     */
    @FunctionalInterface
    public interface ScopeToken extends AutoCloseable {

        /**
         * Close the scope and restore the previous context.
         */
        @Override
        void close();
    }
}
