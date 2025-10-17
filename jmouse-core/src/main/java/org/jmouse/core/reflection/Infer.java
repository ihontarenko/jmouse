package org.jmouse.core.reflection;

import java.lang.reflect.*;

public class Infer {

    private Infer() {}

    public static Class<?> rawClassOf(Type type) {
        if (type instanceof Class<?> rawClass) {
            return rawClass;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getRawType() instanceof Class<?> rawClass ? rawClass : Object.class;
        }

        if (type instanceof GenericArrayType genericArrayType) {
            Class<?> componentType = rawClassOf(genericArrayType.getGenericComponentType());
            return Array.newInstance(componentType, 0).getClass();
        }

        if (type instanceof TypeVariable<?> typeVariable) {
            Type[] bounds = typeVariable.getBounds();
            return bounds.length > 0 ? rawClassOf(bounds[0]) : Object.class;
        }

        if (type instanceof WildcardType wildcardType) {
            Type[] lower = wildcardType.getLowerBounds();
            Type[] upper = wildcardType.getUpperBounds();

            if (lower.length > 0) {
                return rawClassOf(lower[0]);
            }
            if (upper.length > 0) {
                return rawClassOf(upper[0]);
            }
        }

        return Object.class;
    }

    public static Type typeOf(TypeReference<?> typeReference) {
        return typeReference.getType();
    }

}
