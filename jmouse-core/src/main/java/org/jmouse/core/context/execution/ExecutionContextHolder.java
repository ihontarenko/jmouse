package org.jmouse.core.context.execution;

import org.jmouse.core.context.ContextKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

/**
 * 🧭 Thread-bound holder for the current {@link ExecutionContext}.
 *
 * <p>
 * {@code ExecutionContextHolder} maintains a per-thread stack of {@link ExecutionContext}
 * instances. The top of the stack represents the <em>current</em> execution context.
 * If no context is bound to the thread, a shared {@link #ROOT} context is returned.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Maintain a thread-local stack of execution contexts</li>
 *   <li>Provide structured enter/exit semantics via {@link Scope}</li>
 *   <li>Support nested contexts with automatic restoration</li>
 *   <li>Capture and restore context across async boundaries</li>
 * </ul>
 *
 * <h3>Practical examples</h3>
 *
 * <h4>1) Open a context for a request or operation</h4>
 * <pre>{@code
 * ExecutionContext ctx = new DefaultExecutionContext()
 *         .with(ContextKeys.REQUEST_ID, "req-123")
 *         .with(ContextKeys.USER_ID, "u-42");
 *
 * try (ExecutionContextHolder.Scope scope = ExecutionContextHolder.open(ctx)) {
 *     service.handle();
 *     String requestId = ExecutionContextHolder.current()
 *             .get(ContextKeys.REQUEST_ID);
 * }
 * }</pre>
 *
 * <h4>2) Nested context (extend or override keys)</h4>
 * <pre>{@code
 * try (var outer = ExecutionContextHolder.open(baseCtx)) {
 *     ExecutionContext innerCtx = ExecutionContextHolder.current()
 *             .with(ContextKeys.TENANT, "tenant-A");
 *
 *     try (var inner = ExecutionContextHolder.open(innerCtx)) {
 *         worker.doWork();
 *     }
 * }
 * }</pre>
 *
 * <h4>3) Replace the current top context</h4>
 * <pre>{@code
 * ExecutionContextHolder.replace(
 *     ExecutionContextHolder.current()
 *         .with(ContextKeys.DEBUG, true)
 * );
 * }</pre>
 *
 * <h4>4) Propagate context to another thread</h4>
 * <pre>{@code
 * ExecutorService pool = Executors.newFixedThreadPool(4);
 *
 * try (var scope = ExecutionContextHolder.open(ctx)) {
 *     Runnable task = ExecutionContextHolder.wrap(() -> {
 *         audit.log(ExecutionContextHolder.current());
 *     });
 *
 *     pool.submit(task);
 * }
 * }</pre>
 *
 * <h4>5) Wrap a {@link Callable} with context propagation</h4>
 * <pre>{@code
 * Callable<String> task = ExecutionContextHolder.wrap(() -> {
 *     return ExecutionContextHolder.current()
 *             .get(ContextKeys.REQUEST_ID);
 * });
 *
 * Future<String> result = pool.submit(task);
 * }</pre>
 *
 * <h3>Threading model</h3>
 * <ul>
 *   <li>Contexts are bound to threads via {@link ThreadLocal}</li>
 *   <li>No automatic cross-thread propagation</li>
 *   <li>Use {@link #wrap(Runnable)} or {@link #wrap(Callable)} for async execution</li>
 * </ul>
 */
public final class ExecutionContextHolder {

    /**
     * Thread-local stack of execution contexts.
     */
    private static final ThreadLocal<Deque<ExecutionContext>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Shared root context used when no context is bound to the thread.
     */
    private static final ExecutionContext ROOT = new DefaultExecutionContext();

    /**
     * Internal logger for lifecycle diagnostics.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ExecutionContextHolder.class);

    private ExecutionContextHolder() {
    }

    /**
     * Returns the current execution context.
     * <p>
     * If no context is bound to the current thread, {@link #ROOT} is returned.
     * </p>
     *
     * @return the current execution context (never {@code null})
     */
    public static ExecutionContext current() {
        Deque<ExecutionContext> deque = STACK.get();
        return deque.isEmpty() ? ROOT : deque.peek();
    }

    /**
     * Opens (pushes) the given execution context on the current thread stack.
     * <p>
     * The returned {@link Scope} must be closed to restore the previous context.
     * Prefer usage via try-with-resources.
     * </p>
     *
     * @param next execution context to activate
     * @return scope controlling the lifetime of the context
     */
    public static Scope open(ExecutionContext next) {
        Deque<ExecutionContext> deque  = STACK.get();

        deque.push(next);

        return new Scope(next);
    }

    /**
     * Opens an execution context restored from the given snapshot.
     * <p>
     * A new {@link DefaultExecutionContext} is created and populated
     * with the snapshot entries, then pushed via {@link #open(ExecutionContext)}.
     * </p>
     *
     * @param snapshot snapshot to restore
     * @return scope controlling the lifetime of the restored context
     */
    @SuppressWarnings("unchecked")
    public static Scope open(ExecutionSnapshot snapshot) {
        ExecutionContext next = new DefaultExecutionContext();

        for (var entry : snapshot.entries().entrySet()) {
            next = next.with(
                    (ContextKey<Object>) entry.getKey(),
                    entry.getValue()
            );
        }

        return open(next);
    }

    /**
     * Replaces the current top execution context.
     * <p>
     * If the stack is empty, the context is pushed instead.
     * </p>
     *
     * @param next new execution context
     */
    public static void replace(ExecutionContext next) {
        Deque<ExecutionContext> deque  = STACK.get();

        if (deque.isEmpty()) {
            deque.push(next);
            return;
        }

        deque.pop();
        deque.push(next);
    }

    /**
     * Captures a snapshot of the current execution context.
     *
     * @return snapshot of the current context
     */
    public static ExecutionSnapshot capture() {
        return current().snapshot();
    }

    /**
     * Wraps a runnable so that it executes with the current context snapshot installed.
     *
     * @param task runnable to wrap
     * @return wrapped runnable with context propagation
     */
    public static Runnable wrap(Runnable task) {
        ExecutionSnapshot snapshot = capture();
        return () -> {
            try (Scope ignored = open(snapshot)) {
                task.run();
            }
        };
    }

    /**
     * Wraps a callable so that it executes with the current context snapshot installed.
     *
     * @param task callable to wrap
     * @param <V> return value type
     * @return wrapped callable with context propagation
     */
    public static <V> Callable<V> wrap(Callable<V> task) {
        ExecutionSnapshot snapshot = capture();
        return () -> {
            try (Scope ignored = open(snapshot)) {
                return task.call();
            }
        };
    }

    /**
     * Closes the given execution context and restores the previous one.
     * <p>
     * Invoked internally by {@link Scope#close()}.
     * </p>
     *
     * @param expected context expected to be closed
     */
    static void close(ExecutionContext expected) {
        Deque<ExecutionContext> deque = STACK.get();

        if (deque.isEmpty()) {
            return;
        }

        deque.pop();

        if (deque.isEmpty()) {
            STACK.remove();
        }
    }

    private static String shortContext(ExecutionContext context) {
        if (context == null) {
            return "null";
        }

        int size = 0;
        try {
            size = context.entries().size();
        } catch (Exception ignored) { }

        return context.getClass().getSimpleName()
                + "@"
                + Integer.toHexString(System.identityHashCode(context))
                + "(keys=" + size + ")";
    }

    /**
     * 🧩 Auto-closeable execution context scope.
     * <p>
     * Controls the lifetime of a pushed {@link ExecutionContext}.
     * Designed for use with try-with-resources.
     * </p>
     *
     * <p>
     * Closing the scope restores the previous context.
     * Closing is idempotent.
     * </p>
     */
    public static final class Scope implements AutoCloseable {

        private final ExecutionContext executionContext;
        private boolean closed;

        private Scope(ExecutionContext executionContext) {
            this.executionContext = executionContext;
        }

        /**
         * Returns the execution context associated with this scope.
         *
         * @return execution context
         */
        public ExecutionContext context() {
            return executionContext;
        }

        /**
         * Closes this scope and restores the previous execution context.
         */
        @Override
        public void close() {
            if (!closed) {
                ExecutionContextHolder.close(executionContext);
                closed = true;
            }
        }
    }
}
