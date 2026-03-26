package org.jmouse.beans.resolve;

import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.Type;
import java.util.Collection;
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

    public static Type getGenericArgument(Type type) {
        return InferredType.forType(type).getFirst().getType();
    }

}