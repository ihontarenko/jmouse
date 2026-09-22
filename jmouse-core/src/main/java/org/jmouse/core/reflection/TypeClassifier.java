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
     * Checks if the inspected type is an amount of time — {@link java.time.Duration} or
     * {@link java.time.Period}.
     * <p>
     * Both are indivisible values written as a single string ({@code PT15M}, {@code P365D}), not
     * structures to walk into. Without this they classify as beans, and anything binding one goes
     * looking for a no-argument constructor that does not exist — a confusing reflection failure
     * standing in for "this should have gone through a converter".
     * </p>
     *
     * @return {@code true} if the class type is a temporal amount, otherwise {@code false}
     */
    default boolean isTemporalAmount() {
        return is(java.time.Duration.class) || is(java.time.Period.class);
    }

    /**
     * Checks if the inspected type is a value written as a single string rather than a structure to
     * walk into — an address, a moment, a pattern.
     * <p>
     * The same reasoning as {@link #isTemporalAmount()}, carried to the other types that have it. A
     * {@link java.net.URI} has getters for its scheme, host and path, so a binder that has not been
     * told otherwise treats it as a bean, walks into it, finds nothing to set and produces
     * {@code null} — silently, since nothing failed.
     * </p>
     * <p>
     * ⚠️ The set is exactly the types {@code PredefinedConversion} already registers a
     * {@code String} converter for, and widening it beyond them would make things worse rather than
     * better: a type classified as a value with no converter to build it from text turns a confusing
     * reflection failure into a silent {@code null}. {@code UUID}, {@code Path}, {@code Charset},
     * {@code Locale} and {@code LocalDateTime} are all in that position today, which is why they are
     * deliberately absent — each of them needs its converter registered first.
     * </p>
     *
     * @return {@code true} if the class type is a single-string value, otherwise {@code false}
     */
    default boolean isSingleValue() {
        return is(java.net.URI.class)
                || is(java.net.URL.class)
                || is(java.util.regex.Pattern.class)
                || is(java.time.Instant.class)
                || is(java.time.LocalDate.class)
                || is(java.time.ZonedDateTime.class);
    }

    /**
     * Checks if the inspected type is a scalar type.
     * <p>
     * A scalar type is defined as a primitive, a string, a number, a boolean, a byte, a character,
     * an amount of time, or a value written as a single string.
     * </p>
     *
     * @return {@code true} if the class type is scalar, otherwise {@code false}
     */
    default boolean isScalar() {
        return isString() || isNumber() || isBoolean() || isByte() || isCharacter() || isPrimitive()
                || isTemporalAmount() || isSingleValue();
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

    default boolean isAnonymous() {
        return getClassType().isAnonymousClass();
    }

}
