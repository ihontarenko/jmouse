package org.jmouse.core.mapping.factory;

import java.lang.reflect.Constructor;
import java.util.Objects;

public final class ReflectionNoArgsObjectFactory<T> implements ObjectFactory<T> {

    private final Class<T> type;
    private final Constructor<T> constructor;

    public ReflectionNoArgsObjectFactory(Class<T> type) {
        this.type = Objects.requireNonNull(type, "type");
        this.constructor = resolveNoArgumentConstructor(type);
    }

    @Override
    public T create() {
        try {
            return constructor.newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to instantiate: " + type.getName(), ex);
        }
    }

    private static <T> Constructor<T> resolveNoArgumentConstructor(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (Exception exception) {
            throw new IllegalStateException("No-args constructor not found for: " + type.getName(), exception);
        }
    }
}
