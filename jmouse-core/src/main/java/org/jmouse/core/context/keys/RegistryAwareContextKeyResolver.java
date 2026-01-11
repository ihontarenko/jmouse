package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;
import org.jmouse.core.context.ContextKey;
import org.jmouse.core.reflection.Reflections;

public final class RegistryAwareContextKeyResolver implements ContextKeyResolver {

    private final ContextKeyRegistry registry;

    public RegistryAwareContextKeyResolver(ContextKeyRegistry registry) {
        this.registry = Verify.nonNull(registry, "registry");
    }

    @Override
    public Object resolveKeyFor(Object value) {
        Verify.nonNull(value, "value");

        ContextKey<?> directKey = registry.findKeyForValue(value);

        if (directKey != null) {
            return directKey;
        }

        Class<?>      userClass = Reflections.getUserClass(value.getClass());
        ContextKey<?> classKey  = registry.findKeyForUserClass(userClass);

        if (classKey != null) {
            return classKey;
        }

        return userClass;
    }
}
