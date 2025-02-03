package org.jmouse.core.reflection;

import java.util.*;

/**
 * Represents type metadata and provides utility methods for type analysis.
 * <p>
 * This class wraps a {@link Class} and a corresponding {@link JavaType} to provide
 * various type-checking methods useful in reflection-based operations.
 * </p>
 */
public class TypeDescriptor {

    private final Class<?> rawType;
    private final JavaType type;

    /**
     * Constructs a {@link TypeDescriptor} from a raw {@link Class}.
     *
     * @param rawType the raw class type
     */
    public TypeDescriptor(Class<?> rawType) {
        this.rawType = Objects.requireNonNullElse(rawType, Object.class);
        this.type = JavaType.forType(this.rawType);
    }

    /**
     * Constructs a {@link TypeDescriptor} from a {@link JavaType}.
     *
     * @param type the {@link JavaType} instance
     */
    public TypeDescriptor(JavaType type) {
        this.type = type;
        this.rawType = Objects.requireNonNullElse(type.getRawType(), Object.class);
    }

    /**
     * Creates a {@link TypeDescriptor} for a given raw {@link Class}.
     *
     * @param rawType the class type to wrap
     * @return a new {@link TypeDescriptor} instance
     */
    public static TypeDescriptor forClass(Class<?> rawType) {
        return new TypeDescriptor(rawType);
    }

    /**
     * Creates a {@link TypeDescriptor} for a given {@link JavaType}.
     *
     * @param type the {@link JavaType} to wrap
     * @return a new {@link TypeDescriptor} instance
     */
    public static TypeDescriptor forJavaType(JavaType type) {
        return new TypeDescriptor(type);
    }

    public Class<?> getRawType() {
        return rawType;
    }

    public JavaType getType() {
        return type;
    }

    /**
     * Checks if the current type is assignable from the given class.
     *
     * @param clazz the class to check
     * @return {@code true} if the current type is assignable from the given class
     */
    public boolean is(Class<?> clazz) {
        return clazz.isAssignableFrom(rawType);
    }

    public boolean is(TypeDescriptor typeDescriptor) {
        return is(typeDescriptor.getRawType());
    }

    /**
     * Checks if the type represents an array.
     *
     * @return {@code true} if the type is an array
     */
    public boolean isArray() {
        return type.isArray();
    }

    /**
     * Checks if the type represents a {@link Collection}.
     *
     * @return {@code true} if the type is a collection
     */
    public boolean isCollection() {
        return is(Collection.class);
    }

    /**
     * Checks if the type represents a {@link List}.
     *
     * @return {@code true} if the type is a list
     */
    public boolean isList() {
        return is(List.class);
    }

    /**
     * Checks if the type represents a {@link Set}.
     *
     * @return {@code true} if the type is a set
     */
    public boolean isSet() {
        return is(Set.class);
    }

    /**
     * Checks if the type represents a {@link Map}.
     *
     * @return {@code true} if the type is a map
     */
    public boolean isMap() {
        return is(Map.class);
    }

    /**
     * Checks if the type represents an {@link Enum}.
     *
     * @return {@code true} if the type is an enum
     */
    public boolean isEnum() {
        return is(Enum.class);
    }

    /**
     * Checks if the type represents a primitive type.
     *
     * @return {@code true} if the type is primitive
     */
    public boolean isPrimitive() {
        return rawType.isPrimitive();
    }

    /**
     * Checks if the type represents {@code void} or {@link Void}.
     *
     * @return {@code true} if the type is void
     */
    public boolean isVoid() {
        return is(Void.class) || is(void.class);
    }

    /**
     * Checks if the type represents a {@link String}.
     *
     * @return {@code true} if the type is a string
     */
    public boolean isString() {
        return is(String.class);
    }

    /**
     * Checks if the type represents a number (including primitive number types).
     *
     * @return {@code true} if the type is a number
     */
    public boolean isNumber() {
        return is(Number.class) || is(double.class) || is(float.class) || is(int.class) || is(long.class) || is(
                short.class);
    }

    /**
     * Checks if the type represents a boolean (including the primitive {@code boolean}).
     *
     * @return {@code true} if the type is a boolean
     */
    public boolean isBoolean() {
        return is(Boolean.class) || is(boolean.class);
    }

    /**
     * Checks if the type represents a byte (including the primitive {@code byte}).
     *
     * @return {@code true} if the type is a byte
     */
    public boolean isByte() {
        return is(Byte.class) || is(byte.class);
    }

    /**
     * Checks if the type represents a character (including the primitive {@code char}).
     *
     * @return {@code true} if the type is a character
     */
    public boolean isCharacter() {
        return is(Character.class) || is(char.class);
    }

    /**
     * Checks if the type is a simple scalar type (string, number, boolean, byte, character, or primitive).
     *
     * @return {@code true} if the type is a scalar type
     */
    public boolean isScalar() {
        return isString() || isNumber() || isBoolean() || isByte() || isCharacter() || isPrimitive();
    }

    /**
     * Checks if the type is a Object type.
     *
     * @return {@code true} if the type is a Object type
     */
    public boolean isObject() {
        return rawType.isAssignableFrom(Object.class);
    }
}
