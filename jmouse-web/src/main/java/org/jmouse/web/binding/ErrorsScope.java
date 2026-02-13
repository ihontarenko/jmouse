package org.jmouse.web.binding;

import org.jmouse.validator.Errors;

public final class ErrorsScope {

    private static final ThreadLocal<Errors> CURRENT = new ThreadLocal<>();

    public Errors get() {
        return CURRENT.get();
    }

    public ScopeToken open(Errors errors) {
        Errors previous = CURRENT.get();
        CURRENT.set(errors);
        return new ScopeToken(previous);
    }

    public static final class ScopeToken implements AutoCloseable {

        private final Errors  previous;
        private       boolean closed;

        private ScopeToken(Errors previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            closed = true;

            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
