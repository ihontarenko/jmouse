package org.jmouse.core.events;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread-bound storage for current event trace (span stack).
 */
public final class SpanContextHolder {

    private static final ThreadLocal<Deque<EventTrace>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private SpanContextHolder() {
    }

    public static EventTrace current() {
        Deque<EventTrace> deque = STACK.get();
        return deque.isEmpty() ? null : deque.peek();
    }

    public static SpanScope openRootSpan() {
        EventTrace root = EventTrace.root();
        STACK.get().push(root);
        return new SpanScope(root);
    }

    public static SpanScope openChildSpan() {
        EventTrace parent = current();
        EventTrace child  = (parent == null ? EventTrace.root() : parent.child());
        STACK.get().push(child);
        return new SpanScope(child);
    }

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
     * Auto-closeable span scope.
     */
    public static final class SpanScope implements AutoCloseable {

        private final EventTrace trace;
        private       boolean    closed;

        private SpanScope(EventTrace trace) {
            this.trace = trace;
        }

        public EventTrace trace() {
            return trace;
        }

        @Override
        public void close() {
            if (!closed) {
                SpanContextHolder.close(trace);
                closed = true;
            }
        }
    }
}
