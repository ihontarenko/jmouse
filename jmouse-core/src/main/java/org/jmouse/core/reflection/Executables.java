package org.jmouse.core.reflection;

import java.lang.reflect.Executable;

final public class Executables {

    private Executables() {}

    public static Object[] updateArgument(Executable executable, Object[] oldArguments, int index, Object newValue) {
        Class<?>[] parameterTypes = executable.getParameterTypes();
        Object[] newArguments = oldArguments.clone();

        Class<?> expectedType = resolveParameterType(executable, index);

        if (newArguments[index] == newValue) {
            return oldArguments;
        }

        if (newValue == null) {
            if (expectedType.isPrimitive()) {
                throw new IllegalArgumentException(
                        "Argument at index %d cannot be null (primitive %s)"
                                .formatted(index, expectedType.getName())
                );
            }

            newArguments[index] = null;

            return newArguments;
        }

        Class<?> actualType = newValue.getClass();

        if (!expectedType.isPrimitive() && actualType.isPrimitive()) {
            actualType = Reflections.boxType(actualType);
        }

        if (!expectedType.isAssignableFrom(actualType)) {
            throw new IllegalArgumentException(
                    "Argument type mismatch at index %d: expected %s but got %s"
                            .formatted(index, expectedType.getName(), actualType.getName())
            );
        }

        newArguments[index] = newValue;

        return newArguments;
    }

    public static Class<?> resolveParameterType(Executable executable, int index) {
        Class<?>[] types = executable.getParameterTypes();

        if (executable.isVarArgs()) {
            int last = types.length - 1;

            if (index < last) {
                return types[index];
            }

            Class<?> varargType    = types[last];
            Class<?> componentType = varargType.getComponentType();

            return componentType != null ? componentType : varargType;
        }

        if (index >= types.length) {
            throw new IllegalArgumentException(
                    "Index %d beyond parameter count %d for non-varargs method"
                            .formatted(index, types.length)
            );
        }

        return types[index];
    }

}
