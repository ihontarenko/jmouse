package org.jmouse.core.reflection;

import java.util.*;

/**
 * Provides a type inspection utility for analyzing and classifying Java classes.
 */
public interface TypeClassifier {

    /**
     * Returns the class type being inspected.
     *
     * @return the {@link Class} structured representing the inspected type
     */
    Class<?> getClassType();

    /**
     * Checks if the inspected type is assignable from the given class.
     */
    default boolean is(Class<?> clazz) {
        return clazz.isAssignableFrom(getClassType());
    }

    /**
     * Checks if the inspected type is assignable from the given inspector.
     */
    default boolean is(TypeClassifier inspector) {
        return is(inspector.getClassType());
    }

    /**
     * Checks if the inspected type is an array.
     *
     * @return {@code true} if the class type is an array, otherwise {@code false}
     */
    default boolean isArray() {
        return getClassType().isArray();
    }

    /**
     * Checks if the inspected type is a {@link Collection}.
     *
     * @return {@code true} if the class type is a collection, otherwise {@code false}
     */
    default boolean isCollection() {
        return is(Collection.class);
    }

    /**
     * Checks if the inspected type is a {@link Iterable}.
     *
     * @return {@code true} if the class type is a iterable, otherwise {@code false}
     */
    default boolean isIterable() {
        return is(Iterable.class);
    }

    /**
     * Checks if the inspected type is a {@link List}.
     *
     * @return {@code true} if the class type is a list, otherwise {@code false}
     */
    default boolean isList() {
        return is(List.class);
    }

    /**
     * Checks if the inspected type is a {@link Set}.
     *
     * @return {@code true} if the class type is a set, otherwise {@code false}
     */
    default boolean isSet() {
        return is(Set.class);
    }

    /**
     * Checks if the inspected type is a {@link Map}.
     *
     * @return {@code true} if the class type is a map, otherwise {@code false}
     */
    default boolean isMap() {
        return is(Map.class);
    }

    /**
     * Checks if the inspected type is an {@link Enum}.
     *
     * @return {@code true} if the class type is an enum, otherwise {@code false}
     */
    default boolean isEnum() {
        return is(Enum.class);
    }

    /**
     * Checks if the inspected type is a primitive type.
     *
     * @return {@code true} if the class type is primitive, otherwise {@code false}
     */
    default boolean isPrimitive() {
        return getClassType().isPrimitive();
    }

    /**
     * Checks if the inspected type is {@code void} or {@link Void}.
     *
     * @return {@code true} if the class type represents void, otherwise {@code false}
     */
    default boolean isVoid() {
        return is(Void.class) || is(void.class);
    }

    /**
     * Checks if the inspected type is a {@link String}.
     *
     * @return {@code true} if the class type is a string, otherwise {@code false}
     */
    default boolean isString() {
        return is(String.class);
    }

    /**
     * Checks if the inspected type is a {@link Number} or a primitive numeric type.
     *
     * @return {@code true} if the class type is numeric, otherwise {@code false}
     */
    default boolean isNumber() {
        return is(Number.class) || is(double.class) || is(float.class) || is(int.class) || is(long.class) || is(short.class);
    }

    /**
     * Checks if the inspected type is a {@link Boolean} or a primitive boolean.
     *
     * @return {@code true} if the class type is boolean, otherwise {@code false}
     */
    default boolean isBoolean() {
        return is(Boolean.class) || is(boolean.class);
    }

    /**
     * Checks if the inspected type is a {@link Byte} or a primitive byte.
     *
     * @return {@code true} if the class type is a byte, otherwise {@code false}
     */
    default boolean isByte() {
        return is(Byte.class) || is(byte.class);
    }

    /**
     * Checks if the inspected type is a {@link Character} or a primitive char.
     *
     * @return {@code true} if the class type is a character, otherwise {@code false}
     */
    default boolean isCharacter() {
        return is(Character.class) || is(char.class);
    }

    /**
     * Checks if the inspected type is a scalar type.
     * <p>
     * A scalar type is defined as a primitive, a string, a number, a boolean, a byte, or a character.
     * </p>
     *
     * @return {@code true} if the class type is scalar, otherwise {@code false}
     */
    default boolean isScalar() {
        return isString() || isNumber() || isBoolean() || isByte() || isCharacter() || isPrimitive();
    }

    /**
     * Checks if the inspected type is exactly {@link Object}.
     *
     * @return {@code true} if the class type is {@code Object.class}, otherwise {@code false}
     */
    default boolean isObject() {
        return getClassType() == Object.class;
    }

    /**
     * Checks if the inspected type is a {@link Class} itself.
     *
     * @return {@code true} if the class type is a {@code Class}, otherwise {@code false}
     */
    default boolean isClass() {
        return is(Class.class);
    }

    /**
     * Checks if the inspected type is a structured.
     * <p>
     * A structured is defined as a non-scalar, non-primitive, non-collection, non-map,
     * and non-enum type that is not exactly {@code Object.class}.
     * </p>
     *
     * @return {@code true} if the class type represents a structured, otherwise {@code false}
     */
    default boolean isBean() {
        return !isRecord() && !isObject() && !isEnum() && !isArray() && !isCollection() && !isMap() && !isScalar() && !isUnknown();
    }

    /**
     * Checks if the inspected type is a Java record.
     *
     * @return {@code true} if the class type is a record, otherwise {@code false}
     */
    default boolean isRecord() {
        return is(Record.class);
    }

    /**
     * Alias for {@link #isRecord()}
     *
     * @return {@code true} if the class type is a record, otherwise {@code false}
     */
    default boolean isValueObject() {
        return isRecord();
    }

    /**
     * Checks if the inspected type is unknown.
     * <p>
     * This method is useful for cases where the class type is either undefined
     * ({@code null}) or explicitly marked as {@code Unknown.class}.
     * </p>
     *
     * @return {@code true} if the class type is unknown, otherwise {@code false}
     */
    default boolean isUnknown() {
        return getClassType() == null || is(Unknown.class);
    }

}
