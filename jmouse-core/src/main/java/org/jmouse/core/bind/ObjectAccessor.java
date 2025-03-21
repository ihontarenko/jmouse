package org.jmouse.core.bind;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.reflection.JavaType;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNullElseGet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.jmouse.core.reflection.JavaType.forInstance;

/**
 * Represents a data source abstraction that provides methods for accessing and transforming data.
 * <p>
 * This interface supports nested data retrieval, type conversions, and structure identification.
 * It allows working with objects, lists, maps, arrays, and primitive types in a unified way.
 * </p>
 */
public interface ObjectAccessor extends ClassTypeInspector {

    /**
     * A standard wrapper for object accessor operations.
     */
    ObjectAccessorWrapper STANDARD_WRAPPER = new StandardAccessorWrapper();

    /**
     * Ensures that the expected and actual types are compatible.
     *
     * @param message  the error message context
     * @param expected the expected type
     * @param actual   the actual type
     * @throws IllegalArgumentException if the types are not compatible
     */
    static void verifyTypeCompatibility(String message, Class<?> expected, Class<?> actual) {
        if (actual != null && !expected.isAssignableFrom(actual)) {
            throw new IllegalArgumentException("[%s]: type '%s' is not assignable from '%s'".formatted(message, expected, actual));
        }
    }

    /**
     * Creates an ObjectAccessor instance from the given source object.
     *
     * @param source the source object
     * @return an ObjectAccessor instance wrapping the source
     */
    static ObjectAccessor wrapObject(Object source) {
        return STANDARD_WRAPPER.wrap(source);
    }

    /**
     * Retrieves the underlying source object.
     *
     * @return the original source object
     */
    Object unwrap();

    /**
     * Wraps the given source object into an ObjectAccessor.
     *
     * @param source the source object to wrap
     * @return an ObjectAccessor instance wrapping the source
     */
    ObjectAccessor wrap(Object source);

    /**
     * Retrieves a nested ObjectAccessor by property name.
     *
     * @param name the name of the nested data source
     * @return the nested ObjectAccessor
     */
    ObjectAccessor get(String name);

    /**
     * Retrieves a nested ObjectAccessor by index.
     *
     * @param index the index of the nested data source
     * @return the nested ObjectAccessor
     */
    ObjectAccessor get(int index);

    /**
     * Sets a property value by name.
     *
     * @param name  the property name
     * @param value the value to set
     */
    void set(String name, Object value);

    /**
     * Sets a property value by index.
     * <p>
     * The default implementation throws an {@link UnsupportedOperationException},
     * indicating that indexed access is not supported unless overridden.
     * </p>
     *
     * @param index the property index
     * @param value the value to set
     * @throws UnsupportedOperationException if indexed access is not supported
     */
    void set(int index, Object value);

    /**
     * Retrieves a nested ObjectAccessor using a PropertyPath string.
     *
     * @param path the structured property path
     * @return the nested ObjectAccessor
     * @throws NumberFormatException if an indexed path segment contains a non-numeric value
     */
    default ObjectAccessor navigate(String path) {
        return navigate(PropertyPath.forPath(path));
    }

    /**
     * Retrieves a nested ObjectAccessor using a PropertyPath string.
     *
     * @param path the structured property path
     * @param offset the number of initial segments to skip
     * @return the nested ObjectAccessor
     * @throws NumberFormatException if an indexed path segment contains a non-numeric value
     */
    default ObjectAccessor navigate(String path, int offset) {
        return navigate(PropertyPath.forPath(path), offset);
    }

    /**
     * Retrieves a nested ObjectAccessor using the specified PropertyPath with an offset.
     * <p>
     * The method navigates through the nested structure represented by the PropertyPath.
     * If a segment is indexed, it is parsed as an integer; otherwise, it is treated as a property name.
     * </p>
     *
     * @param name   the structured property path
     * @param offset the number of initial segments to skip
     * @return the nested ObjectAccessor corresponding to the path
     * @throws NumberFormatException if an indexed segment contains a non-numeric value
     */
    default ObjectAccessor navigate(PropertyPath name, int offset) {
        ObjectAccessor       nested  = this;
        int                  counter = 0;
        PropertyPath.Entries entries = name.entries();

        if (offset > 0 && name.entries().size() > 1) {
            entries = name.sup(offset).entries();
        }

        for (CharSequence element : entries) {
            PropertyPath.Type type = entries.type(counter);

            if (!type.isEmpty()) {
                if (type.isIndexed() && type.isNumeric()) {
                    int index = Integer.parseInt(element.toString());
                    nested = nested.get(index);
                } else {
                    nested = nested.get(element.toString());
                }
            }

            counter++;
        }

        return nested;
    }

    /**
     * Retrieves a nested ObjectAccessor using the specified PropertyPath.
     *
     * @param name the structured property path
     * @return the nested ObjectAccessor corresponding to the path
     * @throws NumberFormatException if an indexed segment contains a non-numeric value
     */
    default ObjectAccessor navigate(PropertyPath name) {
        return navigate(name, 0);
    }

    /**
     * Injects the given value into the property specified by the property path string.
     *
     * @param name  the structured property name
     * @param value the value to inject
     */
    default void inject(String name, Object value) {
        inject(PropertyPath.forPath(name), value);
    }

    /**
     * Injects the given value into the property specified by the given PropertyPath.
     * <p>
     * If the PropertyPath contains multiple segments, the method navigates to the nested accessor
     * corresponding to all but the last segment and sets the value using the last segment.
     * </p>
     *
     * @param name  the structured property path identifying the property
     * @param value the value to set
     */
    default void inject(PropertyPath name, Object value) {
        if (name.entries().size() > 1) {
            PropertyPath tail = name.tail();
            PropertyPath path = name.sup(1);
            navigate(path).set(tail.path(), value);
        } else {
            set(name.path(), value);
        }
    }

    /**
     * Retrieves a set of keys representing the entries in this ObjectAccessor.
     * <p>
     * For maps, returns the key set as strings. For lists, generates keys in the format "[index]".
     * </p>
     *
     * @return a set of keys as strings
     */
    default Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        if (isMap()) {
            keys = asMap().keySet().stream().map(Object::toString).collect(toUnmodifiableSet());
        } else if (isList()) {
            keys = IntStream.range(0, asList().size()).mapToObj("[%d]"::formatted).collect(toUnmodifiableSet());
        }
        return keys;
    }

    /**
     * Converts the keys in this ObjectAccessor to PropertyPath instances.
     *
     * @return a set of PropertyPath instances representing the keys
     * @see #keySet()
     */
    default Set<PropertyPath> nameSet() {
        return keySet().stream().map(PropertyPath::forPath).collect(toUnmodifiableSet());
    }

    /**
     * Checks if the underlying data source is null.
     *
     * @return true if the source is null; false otherwise
     */
    default boolean isNull() {
        return unwrap() == null;
    }

    /**
     * Checks if the data source is a simple type (String, Number, Boolean, or Character).
     *
     * @return true if the source is a simple type; false otherwise
     */
    default boolean isSimple() {
        return isScalar();
    }

    /**
     * Converts the data source to the specified target type.
     *
     * @param <T>  the target type
     * @param type the class representing the target type
     * @return an instance of the target type
     * @throws IllegalArgumentException if the conversion is not possible
     */
    default <T> T asType(Class<T> type) {
        verifyTypeCompatibility("#as(Class<Type>)", type, getDataType());
        return type.cast(unwrap());
    }

    /**
     * Returns the raw underlying object.
     *
     * @return the raw object
     */
    default Object asObject() {
        return asType(Object.class);
    }

    /**
     * Converts the data source to a String.
     *
     * @return the string representation of the data source
     */
    default String asText() {
        return asType(String.class);
    }

    /**
     * Converts the data source to a Number.
     *
     * @return the numeric value of the data source
     */
    default Number asNumber() {
        return asType(Number.class);
    }

    /**
     * Converts the data source to an array.
     *
     * @return an array representation of the data source
     */
    default Object[] asArray() {
        return asType(Object[].class);
    }

    /**
     * Converts the data source to a Collection.
     *
     * @return a Collection representation of the data source
     */
    default Collection<?> asCollection() {
        return asType(Collection.class);
    }

    /**
     * Converts the data source to a List.
     *
     * @return a List representation of the data source
     */
    default List<?> asList() {
        return asList(Object.class);
    }

    /**
     * Converts the data source to a typed List.
     *
     * @param <T>  the element type
     * @param type the expected element type
     * @return a List of the specified type
     * @throws IllegalArgumentException if there is a type mismatch
     */
    @SuppressWarnings({"unchecked"})
    default <T> List<T> asList(Class<T> type) {
        List<T>  value   = asType(List.class);
        Class<?> generic = forInstance(value).toList().getFirst().getRawType();
        if (generic == null) {
            generic = forInstance(value.getFirst()).getRawType();
        }
        verifyTypeCompatibility("#asList(Type)", type, generic);
        return value;
    }

    /**
     * Converts the data source to a Set.
     *
     * @return a Set representation of the data source
     */
    default Set<?> asSet() {
        return asSet(Object.class);
    }

    /**
     * Converts the data source to a typed Set.
     *
     * @param <T>  the element type
     * @param type the expected element type
     * @return a Set of the specified type
     * @throws IllegalArgumentException if there is a type mismatch
     */
    @SuppressWarnings({"unchecked"})
    default <T> Set<T> asSet(Class<T> type) {
        Set<T>   value   = asType(Set.class);
        Class<?> generic = forInstance(value).toSet().getFirst().getRawType();
        if (generic == null && !value.isEmpty()) {
            generic = forInstance(value.iterator().next()).getRawType();
        }
        verifyTypeCompatibility("#asSet(Type)", type, generic);
        return value;
    }

    /**
     * Converts the data source to a Map.
     *
     * @return a Map representation of the data source
     */
    default Map<?, ?> asMap() {
        return asMap(Object.class, Object.class);
    }

    /**
     * Converts the data source to a typed Map.
     *
     * @param <K>       the key type
     * @param <V>       the value type
     * @param keyType   the expected key type
     * @param valueType the expected value type
     * @return a Map of the specified key and value types
     * @throws IllegalArgumentException if there is a type mismatch
     */
    @SuppressWarnings({"unchecked"})
    default <K, V> Map<K, V> asMap(Class<K> keyType, Class<V> valueType) {
        Map<K, V> value = asType(Map.class);
        JavaType  type  = forInstance(value).toMap();
        verifyTypeCompatibility("#asMap(K, ?)", keyType, type.getFirst().getRawType());
        verifyTypeCompatibility("#asMap(?, V)", valueType, type.getLast().getRawType());
        return value;
    }

    /**
     * Returns the type of the data source.
     *
     * @return the Class representing the data source type
     */
    default Class<?> getDataType() {
        return requireNonNullElseGet(unwrap(), Object::new).getClass();
    }

    /**
     * Returns the class type being inspected.
     *
     * @return the Class representing the inspected type
     */
    @Override
    default Class<?> getClassType() {
        return getDataType();
    }
}

