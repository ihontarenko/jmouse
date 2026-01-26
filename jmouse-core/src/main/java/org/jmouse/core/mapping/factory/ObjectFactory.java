package org.jmouse.core.mapping.factory;

public interface ObjectFactory<T> {
    T create();

    static <T> ObjectFactory<T> forClass(Class<T> type) {
        return new ReflectionNoArgumentsObjectFactory<>(type);
    }
}
