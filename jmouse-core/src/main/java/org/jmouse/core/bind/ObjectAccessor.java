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
public interface ObjectAccessor extends ClassTypeInspector {

    /**
     * Creates a {@link ObjectAccessor} instance from the given source object.
     *
     * @param source the source object
     * @return a {@link ObjectAccessor} instance wrapping the source
     * @throws UnsupportedDataSourceException if the type is unsupported
     */
    static ObjectAccessor wrap(Object source) {
        return null;
//        ObjectAccessor instance = new DummyObjectAccessor(source);
//
//        if (instance.isInstanceOf(PropertyResolver.class)) {
//            instance = new PropertyResolverAccessor((PropertyResolver) source);
//        } else if (instance.isBean()) {
//            instance = new JavaBeanAccessor(source);
//        } else if (instance.isValueObject()) {
//            instance = new ValueObjectAccessor(source);
//        } else if (instance.isScalar() || instance.isCollection() || instance.isMap() || instance.isArray()) {
//            instance = new StandardTypesAccessor(instance.unwrap());
//        }
//
//        if (instance.isNull()) {
//            instance = new NullObjectAccessor();
//        }
//
//        return instance;
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
     * Retrieves a nested {@link ObjectAccessor} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link ObjectAccessor}
     */
    ObjectAccessor get(String name);

    /**
     * Retrieves a nested {@link ObjectAccessor} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link ObjectAccessor}
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
     *
     * <p>The default implementation throws an {@link UnsupportedDataSourceException},
     * indicating that indexed access is not supported unless overridden by an implementation.</p>
     *
     * @param index the property index
     * @param value the value to set
     * @throws UnsupportedDataSourceException if indexed access is not supported
     */
    void set(int index, Object value);

    /**
     * Retrieves a nested {@link ObjectAccessor} using a {@link PropertyPath}.
     *
     * @param path the structured name path
     * @return the nested {@link ObjectAccessor}
     * @throws NumberFormatException if an indexed path contains a non-numeric value
     */
    default ObjectAccessor navigate(String path) {
        return navigate(PropertyPath.forPath(path));
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} using the specified {@link PropertyPath}.
     * <p>
     * The method navigates through the nested structure represented by the given path.
     * Each segment of the path is processed in order. If a segment is indexed (i.e. it represents
     * an array element) and numeric, the method parses the segment as an integer index; otherwise,
     * it treats the segment as a property name.
     * </p>
     *
     * @param name the structured property path used to navigate the nested properties
     * @return the nested {@link ObjectAccessor} corresponding to the given path
     * @throws NumberFormatException if an indexed path segment contains a non-numeric value
     */
    default ObjectAccessor navigate(PropertyPath name) {
        ObjectAccessor       nested  = this;
        int                  counter = 0;
        PropertyPath.Entries entries = name.entries();

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
     * Injects the given value into the property specified by the given property name.
     * <p>
     * This is a convenience method that converts the provided string into a {@link PropertyPath}
     * and then delegates to {@link #inject(PropertyPath, Object)}.
     * </p>
     *
     * @param name  the structured property name as a string
     * @param value the value to inject into the property
     */
    default void inject(String name, Object value) {
        inject(PropertyPath.forPath(name), value);
    }

    /**
     * Injects the given value into the property specified by the {@link PropertyPath}.
     * <p>
     * If the property path consists of multiple segments, the method navigates to the nested
     * accessor corresponding to all but the last segment (using {@link PropertyPath#sup(int)}),
     * and then sets the value using the last segment (obtained via {@link PropertyPath#tail()}).
     * If the property path contains only a single segment, the value is set directly.
     * </p>
     *
     * @param name  the structured property path identifying the property to inject the value into
     * @param value the value to set on the identified property
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
     * Retrieves a list of keys representing the entries in this {@link ObjectAccessor}.
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
     * Retrieves a list of {@link PropertyPath} representations for the keys in this {@link ObjectAccessor}.
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
