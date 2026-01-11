package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;
import org.jmouse.core.context.ContextKey;

import java.util.LinkedHashMap;
import java.util.Map;

final class UserClassOnlyContextKeyRegistry implements ContextKeyRegistry {

    private final Map<Class<?>, ContextKey<?>> mappings;

    UserClassOnlyContextKeyRegistry(Map<Class<?>, ContextKey<?>> mappings) {
        Verify.nonNull(mappings, "mappings");
        this.mappings = new LinkedHashMap<>(mappings);
    }

    @Override
    public ContextKey<?> findKeyForValue(Object value) {
        return null;
    }

    @Override
    public ContextKey<?> findKeyForUserClass(Class<?> userClass) {
        if (userClass == null) {
            return null;
        }
        return mappings.get(userClass);
    }

    @Override
    public String toString() {
        return "UserClassOnlyContextKeyRegistry[size=" + mappings.size() + "]";
    }
}
