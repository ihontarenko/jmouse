package org.jmouse.core.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

public final class ExecutionContextHolder {

    private static final ThreadLocal<Deque<ExecutionContext>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ExecutionContext                     ROOT  = new DefaultExecutionContext();

    private ExecutionContextHolder() {
    }

    public static ExecutionContext current() {
        Deque<ExecutionContext> deque = STACK.get();
        return deque.isEmpty() ? ROOT : deque.peek();
    }

    public static Scope open(ExecutionContext next) {
        STACK.get().push(next);
        return new Scope(next);
    }

    public static Scope open(ExecutionSnapshot snapshot) {
        ExecutionContext next = new DefaultExecutionContext();

        for (var entry : snapshot.entries().entrySet()) {
            next = next.with((ContextKey<Object>) entry.getKey(), entry.getValue());
        }

        return open(next);
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

        deque.pop();

        if (deque.isEmpty()) {
            STACK.remove();
        }
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
