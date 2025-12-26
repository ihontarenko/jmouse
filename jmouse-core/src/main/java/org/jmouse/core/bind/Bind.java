package org.jmouse.core.bind;

import org.jmouse.core.reflection.InferredType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A utility class for binding data from a {@link ObjectAccessor} to Java objects.
 * Provides convenient factory methods and type-safe bindings for common types.
 */
public final class Bind {

    /** A bindable instance for {@code Map<String, String>}. */
    public static final Bindable<Map<String, String>> STRING_MAP  = Bindable.ofMap(String.class, String.class);

    /** A bindable instance for {@code Set<String>}. */
    public static final Bindable<Set<String>>         STRING_SET  = Bindable.ofSet(String.class);

    /** A bindable instance for {@code List<String>}. */
    public static final Bindable<List<String>>        STRING_LIST = Bindable.ofList(String.class);

    private final Binder binder;

    private Bind(Binder binder) {
        this.binder = binder;
    }

    /**
     * Creates a {@code Bind} instance with the given data source.
     *
     * @param data the source data
     * @return a new {@code Bind} instance
     */
    public static Bind with(Object data) {
        return with(ObjectAccessor.wrapObject(data));
    }

    /**
     * Creates a {@code Bind} instance with the specified {@link ObjectAccessor}.
     *
     * @param dataSource the data source
     * @return a new {@code Bind} instance
     */
    public static Bind with(ObjectAccessor dataSource) {
        return with(new Binder(dataSource));
    }

    /**
     * Creates a {@code Bind} instance with the specified {@link ObjectAccessor}
     * and a default {@link BindCallback}.
     *
     * @param dataSource the data source
     * @param callback   the default bind callback to use
     * @return a new {@code Bind} instance
     */
    public static Bind with(ObjectAccessor dataSource, BindCallback callback) {
        Binder binder = new Binder(dataSource);
        binder.setCallback(callback);
        return with(binder);
    }

    /**
     * Creates a {@code Bind} instance with the given {@link Binder}.
     *
     * @param binder the binder instance
     * @return a new {@code Bind} instance
     */
    public static Bind with(Binder binder) {
        return new Bind(binder);
    }

    /**
     * Binds a value from the specified path to a given bindable type.
     *
     * @param path the property path
     * @param bindable the target bindable type
     * @param <T> the expected result type
     * @return the binding result containing the bound value
     */
    public <T> BindResult<T> to(String path, Bindable<T> bindable) {
        return binder.bind(path, bindable);
    }

    /**
     * Binds a value from the specified path to the given target instance.
     *
     * @param path the property path
     * @param target the target instance to bind
     * @param <T> the expected result type
     * @return the binding result containing the bound value
     */
    @SuppressWarnings({"unchecked"})
    public <T> BindResult<T> to(String path, Object target) {
        return (BindResult<T>) binder.bind(path, Bindable.ofInstance(target));
    }

    /**
     * Binds a value to a specified bindable type.
     *
     * @param bindable the target bindable type
     * @param <T> the expected result type
     * @return the binding result containing the bound value
     */
    public <T> BindResult<T> to(Bindable<T> bindable) {
        return binder.bind(bindable);
    }

    /**
     * Binds a value directly to the given target instance.
     *
     * @param target the target instance to bind
     * @param <T> the expected result type
     * @return the binding result containing the bound value
     */
    @SuppressWarnings({"unchecked"})
    public <T> BindResult<T> to(Object target) {
        return (BindResult<T>) binder.bind(Bindable.ofInstance(target));
    }

    /**
     * Binds the value at the given path to the specified {@link InferredType}.
     *
     * @param path the property path
     * @param type the Java type
     * @param <T> the expected result type
     * @return the binding result
     */
    public <T> BindResult<T> to(String path, InferredType type) {
        return to(path, Bindable.of(type));
    }

    /**
     * Binds the value at the given path to the specified class type.
     *
     * @param path the property path
     * @param type the class type
     * @param <T> the expected result type
     * @return the binding result
     */
    public <T> BindResult<T> to(String path, Class<T> type) {
        return to(path, InferredType.forClass(type));
    }

    /**
     * Binds a value to the specified {@link InferredType}.
     *
     * @param type the Java type
     * @param <T> the expected result type
     * @return the binding result
     */
    public <T> BindResult<T> to(InferredType type) {
        return to(Bindable.of(type));
    }

    /**
     * Binds a value to the specified class type.
     *
     * @param type the class type
     * @param <T> the expected result type
     * @return the binding result
     */
    public <T> BindResult<T> to(Class<T> type) {
        return to(InferredType.forClass(type));
    }

    /**
     * Binds a value at the given path as a {@code String}.
     *
     * @param path the property path
     * @return the binding result containing a {@code String}
     */
    public BindResult<String> toString(String path) {
        return to(path, String.class);
    }

    /**
     * Binds a value at the given path as a {@code String}.
     *
     * @param path the property path
     * @return the binding result unwrapped as {@code String}
     */
    public String getString(String path) {
        return get(path, String.class);
    }

    /**
     * Binds a value at the root path as a {@code String}.
     *
     * @return the binding result unwrapped as {@code String}
     */
    public String getString() {
        return get(null, String.class);
    }

    /**
     * Binds a value at the given path as an {@code Integer}.
     *
     * @param path the property path
     * @return the binding result containing an {@code Integer}
     */
    public BindResult<Integer> toInt(String path) {
        return to(path, Integer.class);
    }

    /**
     * Binds a map of {@code String} keys to values of the specified type.
     *
     * @param path the property path
     * @param valueType the class of the map values
     * @param <V> the type of the values
     * @return the binding result containing a {@code Map<String, V>}
     */
    public <V> BindResult<Map<String, V>> toMap(String path, Class<V> valueType) {
        return to(path, Bindable.ofMap(String.class, valueType));
    }

    public <V> Map<String, V> getMap(String path, Class<V> valueType) {
        return toMap(path, valueType).getValue();
    }

    /**
     * Binds a map of {@code String} keys to values of the specified type.
     *
     * @param valueType the class of the map values
     * @param <V> the type of the values
     * @return the binding result containing a {@code Map<String, V>}
     */
    public <V> BindResult<Map<String, V>> toMap(Class<V> valueType) {
        return to(Bindable.ofMap(String.class, valueType));
    }

    public <V> Map<String, V> getMap(Class<V> valueType) {
        return toMap(valueType).getValue();
    }

    /**
     * Binds a map of {@code String} keys to {@code Object} values.
     *
     * @param path the property path
     * @return the binding result containing a {@code Map<String, Object>}
     */
    public BindResult<Map<String, Object>> toMap(String path) {
        return to(path, Bindable.ofMap(String.class, Object.class));
    }

    public Map<String, Object> getMap(String path) {
        return toMap(path).getValue();
    }

    /**
     * Binds a {@code Map<String, String>}.
     *
     * @return the binding result containing a {@code Map<String, String>}
     */
    public BindResult<Map<String, String>> toMap() {
        return to(STRING_MAP);
    }

    public Map<String, String> getMap() {
        return toMap().getValue();
    }

    /**
     * Binds a list of elements of the specified type.
     *
     * @param path the property path
     * @param elementType the class of the list elements
     * @param <V> the element type
     * @return the binding result containing a {@code List<V>}
     */
    public <V> BindResult<List<V>> toList(String path, Class<V> elementType) {
        return to(path, Bindable.ofList(elementType));
    }

    /**
     * Binds a list of elements of the specified type.
     *
     * @param elementType the class of the list elements
     * @param <V> the element type
     * @return the binding result containing a {@code List<V>}
     */
    public <V> BindResult<List<V>> toList(Class<V> elementType) {
        return to(Bindable.ofList(elementType));
    }

    /**
     * Binds a {@code List<String>}.
     *
     * @return the binding result containing a {@code List<String>}
     */
    public BindResult<List<String>> toList() {
        return to(STRING_LIST);
    }

    /**
     * Binds a set of elements of the specified type.
     *
     * @param elementType the class of the set elements
     * @param <V> the element type
     * @return the binding result containing a {@code Set<V>}
     */
    public <V> BindResult<Set<V>> toSet(Class<V> elementType) {
        return to(Bindable.ofSet(elementType));
    }

    /**
     * Binds a {@code Set<String>}.
     *
     * @return the binding result containing a {@code Set<String>}
     */
    public BindResult<Set<String>> toSet() {
        return to(STRING_SET);
    }

    /**
     * Binds a value to a specified bindable type and return.
     *
     * @return the bound object
     */
    public <T> T get(String path, Class<T> type) {
        return to(path, type).getValue();
    }

    /**
     * Binds a value to a specified bindable type and return.
     *
     * @return the bound object
     */
    public <T> T get(Class<T> type) {
        return get(null, type);
    }

}
