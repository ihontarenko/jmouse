package org.jmouse.core.events;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 🧵 Thread-bound storage for the current event trace (span stack).
 *
 * <p>
 * {@code SpanContextHolder} maintains a stack of {@link EventTrace} instances
 * bound to the current thread. It allows events published within the same
 * execution flow to be correlated and nested.
 * </p>
 *
 * <h3>Semantics</h3>
 * <ul>
 *   <li>Each thread has its own independent span stack.</li>
 *   <li>The top of the stack represents the current active span.</li>
 *   <li>Spans are managed via {@link SpanScope} and follow a push/pop model.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * try (SpanScope scope = SpanContextHolder.rootSpan()) {
 *     eventManager.publish(event);
 * }
 * }</pre>
 *
 * <p>
 * This class is intended for synchronous execution flows.
 * It does not propagate spans across threads.
 * </p>
 */
public final class SpanContextHolder {

    /**
     * Thread-local stack of active event traces.
     */
    private static final ThreadLocal<Deque<EventTrace>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private SpanContextHolder() {
    }

    /**
     * Returns the current active event trace.
     *
     * @return the current {@link EventTrace}, or {@code null} if no span is active
     */
    public static EventTrace current() {
        Deque<EventTrace> deque = STACK.get();
        return deque.isEmpty() ? null : deque.peek();
    }

    /**
     * Starts a new root span.
     * <p>
     * A root span begins a new correlation scope and has no parent.
     * The created span becomes the current active span.
     * </p>
     *
     * @return an auto-closeable {@link SpanScope} controlling the span lifetime
     */
    public static SpanScope rootSpan() {
        EventTrace root = EventTrace.root();
        STACK.get().push(root);
        return new SpanScope(root);
    }

    /**
     * Starts a new child span.
     * <p>
     * If a current span exists, the child span is derived from it.
     * Otherwise, a new root span is created.
     * </p>
     *
     * @return an auto-closeable {@link SpanScope} controlling the span lifetime
     */
    public static SpanScope childSpan() {
        EventTrace parent = current();
        EventTrace child = (parent == null ? EventTrace.root() : parent.child());
        STACK.get().push(child);
        return new SpanScope(child);
    }

    /**
     * Closes the given span and removes it from the current thread stack.
     * <p>
     * This method is invoked internally by {@link SpanScope#close()}.
     * If the stack becomes empty, the thread-local storage is cleared.
     * </p>
     *
     * @param expected the span expected to be closed
     */
    static void close(EventTrace expected) {
        Deque<EventTrace> deque = STACK.get();

        if (deque.isEmpty()) {
            return;
        }

        deque.pop();

        if (deque.isEmpty()) {
            STACK.remove();
        }
    }

    /**
     * 🧩 Auto-closeable span scope.
     *
     * <p>
     * {@code SpanScope} controls the lifetime of an {@link EventTrace}
     * using a try-with-resources pattern.
     * </p>
     *
     * <p>
     * Closing the scope removes the associated span from the current
     * thread stack. Closing is idempotent.
     * </p>
     */
    public static final class SpanScope implements AutoCloseable {

        private final EventTrace trace;
        private boolean closed;

        private SpanScope(EventTrace trace) {
            this.trace = trace;
        }

        /**
         * Returns the trace associated with this scope.
         *
         * @return the active {@link EventTrace}
         */
        public EventTrace trace() {
            return trace;
        }

        /**
         * Closes this span scope and restores the previous span, if any.
         */
        @Override
        public void close() {
            if (!closed) {
                SpanContextHolder.close(trace);
                closed = true;
            }
        }
    }
}
