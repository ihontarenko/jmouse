package org.jmouse.core.events;

import org.jmouse.core.trace.SpanScopes;

import java.util.function.Function;
import java.util.function.Supplier;

public final class EventOperations {

    private final EventOperations.TraceScopeRunner DEFAULT_TRACE = this::runWithTrace;

    public enum TraceMode { NONE, ROOT_IF_ABSENT, CHILD }

    @FunctionalInterface
    public interface Emitter<P> {
        void emit(EventName name, P payload);
    }

    @FunctionalInterface
    public interface TraceScopeRunner {
        <T> T run(TraceMode mode, Supplier<T> action);
    }

    private <T> T runWithTrace(TraceMode mode, Supplier<T> operation) {
        return switch (mode) {
            case NONE -> operation.get();
            case ROOT_IF_ABSENT -> SpanScopes.rootIfAbsent(operation);
            case CHILD -> SpanScopes.child(operation);
        };
    }

    public static <P, T> T within(
            TraceMode traceMode,
            TraceScopeRunner tracer,
            Emitter<P> emitter,
            EventName started,
            EventName succeeded,
            EventName failed,
            Supplier<P> startedPayload,
            Supplier<T> action,
            Function<T, P> successPayload,
            Function<Throwable, P> failurePayload
    ) {
        Supplier<T> wrapped = () -> {
            if (started != null) {
                emitter.emit(started, startedPayload.get());
            }
            try {
                T result = action.get();
                if (succeeded != null) {
                    emitter.emit(succeeded, successPayload.apply(result));
                }
                return result;
            } catch (Throwable exception) {
                if (failed != null) {
                    emitter.emit(failed, failurePayload.apply(exception));
                }
                throw exception;
            }
        };

        return tracer.run(traceMode, wrapped);
    }

    private EventOperations() {}
}
