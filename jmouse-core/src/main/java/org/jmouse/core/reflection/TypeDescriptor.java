package org.jmouse.core.reflection;

import java.util.*;

/**
 * Represents type metadata and provides utility methods for type analysis.
 * <p>
 * This class wraps a {@link Class} and a corresponding {@link JavaType} to facilitate
 * various type-checking operations commonly used in reflection-based processing.
 * </p>
 */
public class TypeDescriptor {

    private final Class<?> rawType;
    private final JavaType type;

    /**
     * Constructs a {@link TypeDescriptor} from a raw {@link Class}.
     *
     * @param rawType the class type to be wrapped
     */
    public TypeDescriptor(Class<?> rawType) {
        this.rawType = Objects.requireNonNullElse(rawType, Object.class);
        this.type = JavaType.forType(this.rawType);
    }

    /**
     * Constructs a {@link TypeDescriptor} from a {@link JavaType}.
     *
     * @param type the {@link JavaType} instance to be wrapped
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
     * @param type the {@link JavaType} instance to wrap
     * @return a new {@link TypeDescriptor} instance
     */
    public static TypeDescriptor forJavaType(JavaType type) {
        return new TypeDescriptor(type);
    }

    /**
     * Returns the raw class type.
     *
     * @return the underlying class type
     */
    public Class<?> getRawType() {
        return rawType;
    }

    /**
     * Returns the wrapped {@link JavaType} instance.
     *
     * @return the wrapped Java type
     */
    public JavaType getType() {
        return type;
    }

    /**
     * Checks if this type is assignable from the given class.
     *
     * @param clazz the class to check
     * @return {@code true} if this type is assignable from the given class
     */
    public boolean is(Class<?> clazz) {
        return clazz.isAssignableFrom(rawType);
    }

    /**
     * Checks if this type is assignable from another {@link TypeDescriptor}.
     *
     * @param typeDescriptor the type descriptor to check
     * @return {@code true} if this type is assignable from the given descriptor
     */
    public boolean is(TypeDescriptor typeDescriptor) {
        return is(typeDescriptor.getRawType());
    }

    /** Checks if the type is an array. */
    public boolean isArray() {
        return type.isArray();
    }

    /** Checks if the type represents a {@link Collection}. */
    public boolean isCollection() {
        return is(Collection.class);
    }

    /** Checks if the type represents a {@link List}. */
    public boolean isList() {
        return is(List.class);
    }

    /** Checks if the type represents a {@link Set}. */
    public boolean isSet() {
        return is(Set.class);
    }

    /** Checks if the type represents a {@link Map}. */
    public boolean isMap() {
        return is(Map.class);
    }

    /** Checks if the type represents an {@link Enum}. */
    public boolean isEnum() {
        return is(Enum.class);
    }

    /** Checks if the type is a primitive type. */
    public boolean isPrimitive() {
        return rawType.isPrimitive();
    }

    /** Checks if the type represents {@code void} or {@link Void}. */
    public boolean isVoid() {
        return is(Void.class) || is(void.class);
    }

    /** Checks if the type represents a {@link String}. */
    public boolean isString() {
        return is(String.class);
    }

    /** Checks if the type represents a number (including primitive number types). */
    public boolean isNumber() {
        return is(Number.class) || is(double.class) || is(float.class) || is(int.class) || is(long.class) || is(short.class);
    }

    /** Checks if the type represents a boolean (including {@code boolean} primitive). */
    public boolean isBoolean() {
        return is(Boolean.class) || is(boolean.class);
    }

    /** Checks if the type represents a byte (including {@code byte} primitive). */
    public boolean isByte() {
        return is(Byte.class) || is(byte.class);
    }

    /** Checks if the type represents a character (including {@code char} primitive). */
    public boolean isCharacter() {
        return is(Character.class) || is(char.class);
    }

    /**
     * Checks if the type is a scalar type (i.e., {@link String}, {@link Number},
     * {@link Boolean}, {@link Byte}, {@link Character}, or any primitive type).
     *
     * @return {@code true} if the type is scalar
     */
    public boolean isScalar() {
        return isString() || isNumber() || isBoolean() || isByte() || isCharacter() || isPrimitive();
    }

    /**
     * Checks if the type represents {@code Object.class}.
     * <p>
     * This method verifies whether the underlying type is explicitly {@code Object.class},
     * rather than checking for general assignability.
     * </p>
     *
     * @return {@code true} if the type is exactly {@code Object.class}, otherwise {@code false}
     */
    public boolean isObject() {
        return rawType == Object.class;
    }

    /**
     * Checks if the type represents a {@link Class} instance.
     * <p>
     * This method determines whether the underlying type itself is a {@code Class<?>} type,
     * meaning it can hold references to other class definitions.
     * </p>
     *
     * @return {@code true} if the type is a {@code Class<?>}, otherwise {@code false}
     */
    public boolean isClass() {
        return is(Class.class);
    }

    /**
     * Checks if the type represents a JavaBean.
     * <p>
     * A type is considered a JavaBean if it is neither a primitive nor a collection-like structure,
     * nor a scalar type such as {@link String} or {@link Number}. This method is useful when determining
     * whether an object follows the JavaBean pattern.
     * </p>
     *
     * @return {@code true} if the type is a JavaBean, otherwise {@code false}
     */
    public boolean isBean() {
        return !isObject() && !isEnum() && !isArray() && !isCollection() && !isMap() && !isScalar();
    }

    /**
     * Checks if the type is a {@code Record} type.
     *
     * @return {@code true} if the type is a record
     */
    public boolean isRecord() {
        return is(Record.class);
    }

    @Override
    public String toString() {
        return "TypeDescriptor: [%s]".formatted(type);
    }
}
