package org.jmouse.core.bind;

import org.jmouse.core.env.PropertyResolver;
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
public interface PropertyValuesAccessor extends ClassTypeInspector {

    /**
     * Creates a {@link PropertyValuesAccessor} instance from the given source object.
     *
     * @param source the source object
     * @return a {@link PropertyValuesAccessor} instance wrapping the source
     * @throws UnsupportedDataSourceException if the type is unsupported
     */
    static PropertyValuesAccessor wrap(Object source) {
        PropertyValuesAccessor instance = new DummyPropertyValuesAccessor(source);

        if (instance.isInstanceOf(PropertyResolver.class)) {
            instance = new PropertyResolverDataSource((PropertyResolver) source);
        } else if (instance.isBean()) {
            instance = new JavaBeanPropertyValuesAccessor(source);
        } else if (instance.isScalar() || instance.isCollection() || instance.isMap() || instance.isArray()) {
            instance = new StandardPropertyValuesAccessor(instance.unwrap());
        }

        if (instance.isNull()) {
            instance = new NullPropertyValuesAccessor();
        }

        return instance;
    }

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
            throw new IllegalArgumentException(
                    "[%s]: type '%s' is not assignable from '%s'".formatted(message, expected, actual));
        }
    }

    /**
     * Retrieves the underlying source object.
     *
     * @return the original source object
     */
    Object unwrap();

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link PropertyValuesAccessor}
     */
    PropertyValuesAccessor get(String name);

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link PropertyValuesAccessor}
     */
    PropertyValuesAccessor get(int index);

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} using a {@link PropertyPath}.
     *
     * @param path the structured name path
     * @return the nested {@link PropertyValuesAccessor}
     * @throws NumberFormatException if an indexed path contains a non-numeric value
     */
    default PropertyValuesAccessor navigate(String path) {
        return navigate(PropertyPath.forPath(path));
    }

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} using a {@link PropertyPath}.
     *
     * @param name the structured name path
     * @return the nested {@link PropertyValuesAccessor}
     * @throws NumberFormatException if an indexed path contains a non-numeric value
     */
    default PropertyValuesAccessor navigate(PropertyPath name) {
        PropertyValuesAccessor nested  = this;
        int                    counter = 0;
        PropertyPath.Entries   entries = name.entries();

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
     * Retrieves a list of keys representing the entries in this {@link PropertyValuesAccessor}.
     * <p>
     * If the data source is a map, the method returns its key set as strings.
     * If the data source is a list, the method generates index-based keys
     * in the format {@code [index]}.
     * </p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * DataSource source = ...;
     * List<String> keys = source.keys();
     * }</pre>
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
     * Retrieves a list of {@link PropertyPath} representations for the keys in this {@link PropertyValuesAccessor}.
     * <p>
     * This method converts the keys obtained from {@link #keySet()} into {@link PropertyPath} objects,
     * allowing structured representation of hierarchical data.
     * </p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * DataSource source = ...;
     * List<PropertyPath> names = source.names();
     * }</pre>
     *
     * @return a set of {@link PropertyPath} instances representing the keys
     * @see #keySet()
     */
    default Set<PropertyPath> nameSet() {
        return keySet().stream().map(PropertyPath::forPath).collect(toUnmodifiableSet());
    }

    /**
     * Checks if the data source is {@code null}.
     *
     * @return {@code true} if the underlying source is {@code null}, otherwise {@code false}
     */
    default boolean isNull() {
        return unwrap() == null;
    }

    /**
     * Checks if the data source is assignable to the specified type.
     *
     * @param <T>  the expected type
     * @param type the class to check
     * @return {@code true} if assignable, otherwise {@code false}
     */
    default <T> boolean isInstanceOf(Class<T> type) {
        return type.isAssignableFrom(getDataType());
    }

    /**
     * Checks if the data source is a simple type (String, Number, Boolean, or Character).
     *
     * @return {@code true} if the data source is a simple type, otherwise {@code false}
     */
    default boolean isSimple() {
        return isScalar();
    }

    /**
     * Converts the data source to a specified type.
     *
     * @param <T>  the target type
     * @param type the class representing the target type
     * @return an instance of the target type
     * @throws IllegalArgumentException if the types are not compatible
     */
    default <T> T asType(Class<T> type) {
        verifyTypeCompatibility("#as(Class<Type>)", type, getDataType());
        return type.cast(unwrap());
    }

    /**
     * Returns the raw object stored in the data source.
     *
     * @return the raw object
     */
    default Object asObject() {
        return asType(Object.class);
    }

    /**
     * Converts the data source to a {@link String}.
     *
     * @return the string representation of the data source
     */
    default String asText() {
        return asType(String.class);
    }

    /**
     * Converts the data source to a {@link Number}.
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
     * Converts the data source to a {@link Collection}.
     *
     * @return a list representation of the data source
     */
    default Collection<?> asCollection() {
        return asType(Collection.class);
    }

    /**
     * Converts the data source to a {@link List}.
     *
     * @return a list representation of the data source
     */
    default List<?> asList() {
        return asList(Object.class);
    }

    /**
     * Converts the data source to a typed {@link List}.
     *
     * @param <T>  the element type
     * @param type the expected element type
     * @return a typed list representation of the data source
     * @throws IllegalArgumentException if type mismatch occurs
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
     * Converts the data source to a {@link Set}.
     *
     * @return a set representation of the data source
     */
    default Set<?> asSet() {
        return asSet(Object.class);
    }

    /**
     * Converts the data source to a typed {@link Set}.
     *
     * @param <T>  the element type
     * @param type the expected element type
     * @return a typed set representation of the data source
     * @throws IllegalArgumentException if type mismatch occurs
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
     * Converts the data source to a {@link Map}.
     *
     * @return a map representation of the data source
     */
    default Map<?, ?> asMap() {
        return asMap(Object.class, Object.class);
    }

    /**
     * Converts the data source to a typed {@link Map}.
     *
     * @param <K>       the key type
     * @param <V>       the value type
     * @param keyType   the expected key type
     * @param valueType the expected value type
     * @return a typed map representation of the data source
     * @throws IllegalArgumentException if type mismatch occurs
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
     * @return the {@link Class} representing the data source type
     */
    default Class<?> getDataType() {
        return requireNonNullElseGet(unwrap(), Object::new).getClass();
    }

    /**
     * Returns the class type being inspected.
     *
     * @return the {@link Class} structured representing the inspected type
     */
    @Override
    default Class<?> getClassType() {
        return getDataType();
    }

}
