package org.jmouse.core.context.keys;

import org.jmouse.core.context.ContextKey;

final class NoopContextKeyRegistry implements ContextKeyRegistry {

    static final NoopContextKeyRegistry INSTANCE = new NoopContextKeyRegistry();

    private NoopContextKeyRegistry() {
    }

    @Override
    public ContextKey<?> findKeyForValue(Object value) {
        return null;
    }

    @Override
    public ContextKey<?> findKeyForUserClass(Class<?> userClass) {
        return null;
    }
}
