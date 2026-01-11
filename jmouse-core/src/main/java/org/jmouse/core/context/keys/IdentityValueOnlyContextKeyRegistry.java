package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;
import org.jmouse.core.context.ContextKey;

import java.util.IdentityHashMap;
import java.util.Map;

final class IdentityValueOnlyContextKeyRegistry implements ContextKeyRegistry {

    private final Map<Object, ContextKey<?>> mappings;

    IdentityValueOnlyContextKeyRegistry(Map<Object, ContextKey<?>> mappings) {
        Verify.nonNull(mappings, "mappings");
        this.mappings = new IdentityHashMap<>(mappings);
    }

    @Override
    public ContextKey<?> findKeyForValue(Object value) {
        if (value == null) {
            return null;
        }
        return mappings.get(value);
    }

    @Override
    public ContextKey<?> findKeyForUserClass(Class<?> userClass) {
        return null;
    }

    @Override
    public String toString() {
        return "IdentityValueOnlyContextKeyRegistry[size=" + mappings.size() + "]";
    }
}
