package org.jmouse.core.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

public final class ExecutionContextHolder {

    private static final ThreadLocal<Deque<ExecutionContext>> STACK  = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ExecutionContext                     ROOT   = new DefaultExecutionContext();
    private static final Logger                               LOGGER = LoggerFactory.getLogger(ExecutionContextHolder.class);

    private ExecutionContextHolder() {
    }

    public static ExecutionContext current() {
        Deque<ExecutionContext> deque = STACK.get();
        return deque.isEmpty() ? ROOT : deque.peek();
    }

    public static Scope open(ExecutionContext next) {
        Deque<ExecutionContext> deque  = STACK.get();
        int                     before = deque.size();

        deque.push(next);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("open(): depth {} -> {}, top={}", before, deque.size(), shortContext(next));
        }

        return new Scope(next);
    }

    @SuppressWarnings("unchecked")
    public static Scope open(ExecutionSnapshot snapshot) {
        ExecutionContext next = new DefaultExecutionContext();

        for (var entry : snapshot.entries().entrySet()) {
            next = next.with((ContextKey<Object>) entry.getKey(), entry.getValue());
        }

        return open(next);
    }

    public static void replace(ExecutionContext next) {
        Deque<ExecutionContext> deque  = STACK.get();
        int                     before = deque.size();

        if (deque.isEmpty()) {
            deque.push(next);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("replace(): stack empty -> push, depth {} -> {}, top={}",
                        before, deque.size(), shortContext(next));
            }
            return;
        }

        ExecutionContext previous = deque.peek();

        deque.pop();
        deque.push(next);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("replace(): depth {}, previousTop={} -> newTop={}",
                    before, shortContext(previous), shortContext(next));
        }
    }

    public static ExecutionSnapshot capture() {
        return current().snapshot();
    }

    public static Runnable wrap(Runnable task) {
        ExecutionSnapshot snapshot = capture();
        return () -> {
            try (Scope ignored = open(snapshot)) {
                task.run();
            }
        };
    }

    public static <V> Callable<V> wrap(Callable<V> task) {
        ExecutionSnapshot snapshot = capture();
        return () -> {
            try (Scope ignored = open(snapshot)) {
                return task.call();
            }
        };
    }

    static void close(ExecutionContext expected) {
        Deque<ExecutionContext> deque = STACK.get();

        if (deque.isEmpty()) {
            return;
        }

        int              before = deque.size();
        ExecutionContext top    = deque.peek();

        deque.pop();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("close(): depth {} -> {}, topWas={}, expected={}",
                    before, deque.size(), shortContext(top), shortContext(expected));
        }

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

        return context.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(context)) + "(keys=" + size + ")";
    }

    public static final class Scope implements AutoCloseable {

        private final ExecutionContext executionContext;
        private       boolean          closed;

        private Scope(ExecutionContext executionContext) {
            this.executionContext = executionContext;
        }

        public ExecutionContext context() {
            return executionContext;
        }

        @Override
        public void close() {
            if (!closed) {
                ExecutionContextHolder.close(executionContext);
                closed = true;
            }
        }
    }
}
