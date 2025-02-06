package org.jmouse.context.bind;

import org.jmouse.core.reflection.JavaType;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNullElseGet;
import static org.jmouse.core.reflection.JavaType.forInstance;

/**
 * Represents a data source abstraction that provides methods for accessing and transforming data.
 * <p>
 * This interface supports nested data retrieval, type conversions, and structure identification.
 * It allows working with objects, lists, maps, arrays, and primitive types in a unified way.
 * </p>
 */
public interface DataSource {

    /**
     * Retrieves the underlying source object.
     *
     * @return the original source object
     */
    Object getSource();

    /**
     * Retrieves a nested {@link DataSource} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link DataSource}
     */
    DataSource get(String name);

    /**
     * Retrieves a nested {@link DataSource} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link DataSource}
     */
    DataSource get(int index);

    /**
     * Retrieves a nested {@link DataSource} using a {@link NamePath}.
     *
     * @param name the structured name path
     * @return the nested {@link DataSource}
     * @throws NumberFormatException if an indexed path contains a non-numeric value
     * <p>
     * Example usage:
     * <pre>{@code
     * DataSource source = ...;
     * NamePath path = NamePath.of("user.address.street");
     * DataSource street = source.get(path);
     * }</pre>
     */
    default DataSource get(NamePath name) {
        DataSource       nested   = this;
        int              counter  = 0;
        NamePath.Entries entries = name.entries();

        for (CharSequence element : entries) {
            NamePath.Type type = entries.type(counter);

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
     * Retrieves a collection of keys representing the entries in this {@link DataSource}.
     *
     * @return a collection of keys as strings
     * <p>
     * Example usage:
     * <pre>{@code
     * DataSource source = ...;
     * Collection<String> keys = source.keys();
     * }</pre>
     */
    default List<String> keys() {
        List<String> keys = new ArrayList<>();

        if (isMap()) {
            keys = asMap().keySet().stream().map(Object::toString).toList();
        } else if (isList()) {
            keys = IntStream.range(0, asList().size()).mapToObj("[%d]"::formatted).toList();
        }

        return keys;
    }

    default List<NamePath> names() {
        return keys().stream().map(NamePath::new).toList();
    }

    /**
     * Checks if the data source is {@code null}.
     *
     * @return {@code true} if the underlying source is {@code null}, otherwise {@code false}
     */
    default boolean isNull() {
        return getSource() == null;
    }

    /**
     * Checks if the data source is assignable to the specified type.
     *
     * @param <T>  the expected type
     * @param type the class to check
     * @return {@code true} if assignable, otherwise {@code false}
     */
    default <T> boolean is(Class<T> type) {
        return type.isAssignableFrom(getType());
    }

    /**
     * Checks if the data source is a String
     *
     * @return {@code true} if the data source is a string type, otherwise {@code false}
     */
    default boolean isString() {
        return is(String.class);
    }

    /**
     * Checks if the data source is a Number
     *
     * @return {@code true} if the data source is a numeric type, otherwise {@code false}
     */
    default boolean isNumber() {
        return is(Number.class);
    }

    /**
     * Checks if the data source is a Boolean
     *
     * @return {@code true} if the data source is a boolean type, otherwise {@code false}
     */
    default boolean isBoolean() {
        return is(Boolean.class);
    }

    /**
     * Checks if the data source is a simple type (String, Number, Boolean, or Character).
     *
     * @return {@code true} if the data source is a simple type, otherwise {@code false}
     */
    default boolean isSimple() {
        return isString() || isNumber() || isBoolean() || is(Character.class);
    }

    /**
     * Checks if the data source is a {@link List}.
     *
     * @return {@code true} if the data source is a list, otherwise {@code false}
     */
    default boolean isList() {
        return is(List.class);
    }

    /**
     * Checks if the data source is a {@link Set}.
     *
     * @return {@code true} if the data source is a set, otherwise {@code false}
     */
    default boolean isSet() {
        return is(Set.class);
    }

    /**
     * Checks if the data source is a {@link Collection}.
     *
     * @return {@code true} if the data source is a collection, otherwise {@code false}
     */
    default boolean isCollection() {
        return isList() || isSet();
    }

    /**
     * Checks if the data source is a {@link Map}.
     *
     * @return {@code true} if the data source is a map, otherwise {@code false}
     */
    default boolean isMap() {
        return is(Map.class);
    }

    /**
     * Checks if the data source is an array.
     *
     * @return {@code true} if the data source is an array, otherwise {@code false}
     */
    default boolean isArray() {
        return is(Object[].class);
    }

    /**
     * Converts the data source to a specified type.
     *
     * @param <T>  the target type
     * @param type the class representing the target type
     * @return an instance of the target type
     * @throws IllegalArgumentException if the types are not compatible
     */
    default <T> T as(Class<T> type) {
        ensureTypes("#as(Class<Type>)", type, getType());
        return type.cast(getSource());
    }

    /**
     * Returns the raw object stored in the data source.
     *
     * @return the raw object
     */
    default Object getRaw() {
        return as(Object.class);
    }

    /**
     * Converts the data source to a {@link String}.
     *
     * @return the string representation of the data source
     */
    default String asString() {
        return as(String.class);
    }

    /**
     * Converts the data source to a {@link Number}.
     *
     * @return the numeric value of the data source
     */
    default Number asNumber() {
        return as(Number.class);
    }

    /**
     * Converts the data source to an array.
     *
     * @return an array representation of the data source
     */
    default Object[] asArray() {
        return as(Object[].class);
    }


    /**
     * Converts the data source to a {@link Collection}.
     *
     * @return a list representation of the data source
     */
    default Collection<?> asCollection() {
        return as(Collection.class);
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
        List<T>  value   = as(List.class);
        Class<?> generic = forInstance(value).toList().getFirst().getRawType();

        if (generic == null) {
            generic  = forInstance(value.getFirst()).getRawType();
        }

        ensureTypes("#asList(Type)", type, generic);

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
        Map<K, V> value = as(Map.class);
        JavaType  type  = forInstance(value).toMap();

        ensureTypes("#asMap(K, ?)", keyType, type.getFirst().getRawType());
        ensureTypes("#asMap(?, V)", valueType, type.getLast().getRawType());

        return value;
    }

    /**
     * Returns the type of the data source.
     *
     * @return the {@link Class} representing the data source type
     */
    default Class<?> getType() {
        return requireNonNullElseGet(getSource(), Object::new).getClass();
    }

    /**
     * Creates a {@link DataSource} instance from the given source object.
     *
     * @param source the source object
     * @return a {@link DataSource} instance wrapping the source
     * @throws UnsupportedDataSourceException if the type is unsupported
     */
    static DataSource of(Object source) {
        DataSource instance = new DummyDataSource(source);
        boolean    unknown  = !instance.isArray() && !instance.isCollection() && !instance.isMap() && !instance.isSimple();

        if (unknown && !instance.isNull()) {
            instance = new JavaBeanInstanceDataSource(source);
        } else if (instance.isSimple() || instance.isCollection() || instance.isMap() || instance.isArray()) {
            instance = new StandardDataSource(instance.getSource());
        }

        if (instance.isNull()) {
            instance = new NullValueDataSource();
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
    static void ensureTypes(String message, Class<?> expected, Class<?> actual) {
        if (actual != null && !expected.isAssignableFrom(actual)) {
            throw new IllegalArgumentException(
                    "[%s]: type '%s' is not assignable from '%s'"
                            .formatted(message, expected, actual));
        }
    }

}
