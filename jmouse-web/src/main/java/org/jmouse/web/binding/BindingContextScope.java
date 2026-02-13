package org.jmouse.web.binding;

public final class BindingContextScope {

    private static final ThreadLocal<BindingContext> CURRENT = new ThreadLocal<>();

    public ScopeToken open(BindingContext context) {
        BindingContext previous = CURRENT.get();

        CURRENT.set(context);

        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    public BindingContext current() {
        return CURRENT.get();
    }

    @FunctionalInterface
    public interface ScopeToken extends AutoCloseable {
        @Override
        void close();
    }
}
