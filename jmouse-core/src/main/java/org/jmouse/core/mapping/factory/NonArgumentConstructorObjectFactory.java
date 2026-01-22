package org.jmouse.core.mapping.factory;

import java.lang.reflect.Constructor;

public final class NonArgumentConstructorObjectFactory {

    private NonArgumentConstructorObjectFactory() {}

    public static <T> ObjectFactory<T> forClass(Class<T> type) {
        Constructor<T> ctor;
        try {
            ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
        } catch (Exception ex) {
            throw new IllegalStateException("No default constructor found for: " + type.getName(), ex);
        }

        return () -> {
            try {
                return ctor.newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to instantiate: " + type.getName(), ex);
            }
        };
    }
}
