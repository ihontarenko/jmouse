package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;
import org.jmouse.core.context.ContextKey;

final class CompositeContextKeyRegistry implements ContextKeyRegistry {

    private final ContextKeyRegistry[] registries;

    CompositeContextKeyRegistry(ContextKeyRegistry[] registries) {
        Verify.nonNull(registries, "registries");
        this.registries = registries.clone();
    }

    @Override
    public ContextKey<?> findKeyForValue(Object value) {
        for (ContextKeyRegistry registry : registries) {
            if (registry == null) {
                continue;
            }
            ContextKey<?> key = registry.findKeyForValue(value);
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    @Override
    public ContextKey<?> findKeyForUserClass(Class<?> userClass) {
        for (ContextKeyRegistry registry : registries) {
            if (registry == null) {
                continue;
            }
            ContextKey<?> key = registry.findKeyForUserClass(userClass);
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "CompositeContextKeyRegistry[count=" + registries.length + "]";
    }
}
