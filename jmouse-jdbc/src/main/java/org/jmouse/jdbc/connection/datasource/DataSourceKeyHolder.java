package org.jmouse.jdbc.connection.datasource;

/** Synchronous thread routing context. Prefer withKey for exception-safe, nested use. */
public final class DataSourceKeyHolder {

    private static final ThreadLocal<String> KEY = new ThreadLocal<>();
    private static final ThreadLocal<Scope> SCOPE = new ThreadLocal<>();

    private DataSourceKeyHolder() {}

    /** Legacy manual binding; callers must restore or clear it themselves. */
    public static void use(String key) {
        if (key == null) {
            KEY.remove();
        } else {
            KEY.set(key);
        }
    }

    public static String current() {
        return KEY.get();
    }

    public static void clear() {
        KEY.remove();
    }

    public static Scope withKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("DataSource key must not be blank");
        }
        Scope scope = new Scope(Thread.currentThread(), current(), SCOPE.get());
        SCOPE.set(scope);
        KEY.set(key);
        return scope;
    }

    /** Restores the previous key on its owning thread, in reverse creation order. */
    public static final class Scope implements AutoCloseable {

        private final Thread owner;
        private final String previousKey;
        private final Scope previousScope;
        private boolean closed;

        private Scope(Thread owner, String previousKey, Scope previousScope) {
            this.owner = owner;
            this.previousKey = previousKey;
            this.previousScope = previousScope;
        }

        @Override
        public void close() {
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("Routing scope must close on its owning thread");
            }
            if (closed) {
                return;
            }
            if (SCOPE.get() != this) {
                throw new IllegalStateException("Routing scopes must close in reverse creation order");
            }
            use(previousKey);
            if (previousScope == null) {
                SCOPE.remove();
            } else {
                SCOPE.set(previousScope);
            }
            closed = true;
        }
    }
}
