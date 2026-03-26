package org.jmouse.beans.resolve;

import jakarta.inject.Provider;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class BeanTypes {

    private BeanTypes() {
    }

    public static Class<?> rawType(Type type) {
        return InferredType.forType(type).getClassType();
    }

    public static boolean isOptional(Type type) {
        return Optional.class == rawType(type);
    }

    public static boolean isCollection(Type type) {
        return Collection.class.isAssignableFrom(rawType(type));
    }

    public static boolean isProvider(Type type) {
        return Provider.class.isAssignableFrom(rawType(type));
    }

    public static boolean isMap(Type type) {
        return Map.class.isAssignableFrom(rawType(type));
    }

    public static Type getFirstGeneric(Type type) {
        return getGenericArgument(type, 0);
    }

    public static Type getGenericArgument(Type type, int index) {
        return InferredType.forType(type).getGeneric(index).getType();
    }

}