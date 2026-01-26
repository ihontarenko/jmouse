package org.jmouse.core.mapping.factory;

import org.jmouse.core.Verify;

import java.lang.reflect.Constructor;

public final class ReflectionNoArgumentsObjectFactory<T> implements ObjectFactory<T> {

    private final Class<T>       type;
    private final Constructor<T> constructor;

    public ReflectionNoArgumentsObjectFactory(Class<T> type) {
        this.type = Verify.nonNull(type, "type");
        this.constructor = resolveNoArgumentConstructor(type);
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

    @Override
    public T create() {
        try {
            return constructor.newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to instantiate: " + type.getName(), ex);
        }
    }
}
