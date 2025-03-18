package org.jmouse.core.bind;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeInformation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Represents a bindable value with an associated {@link JavaType}.
 * <p>
 * This class allows deferred retrieval of values via a {@link Supplier}, making it useful
 * for dynamic type binding and lazy initialization.
 * </p>
 *
 * @param <T> the type of the bindable value
 */
public final class Bindable<T> {

    private final JavaType    type;
    private final Supplier<T> value;

    /**
     * Creates a new {@link Bindable} instance with a specified type and value supplier.
     *
     * @param type  the {@link JavaType} representing the type of the value
     * @param value a {@link Supplier} providing the value
     */
    public Bindable(JavaType type, Supplier<T> value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a new {@link Bindable} instance with a specified type and a default {@code null} supplier.
     *
     * @param type the {@link JavaType} representing the type of the value
     */
    public Bindable(JavaType type) {
        this(type, () -> null);
    }

    /**
     * Creates a new {@link Bindable} instance for a given raw class type.
     *
     * @param rawType the class type to wrap
     */
    public Bindable(Class<T> rawType) {
        this(JavaType.forClass(rawType));
    }

    /**
     * Creates a {@link Bindable} instance representing a {@link Map} with the specified key and value types.
     *
     * @param keyType   the class type of the map keys
     * @param valueType the class type of the map values
     * @param <K>       the type of the keys in the map
     * @param <V>       the type of the values in the map
     * @return a {@link Bindable} representing a map with the given key-value types
     */
    public static <K, V> Bindable<Map<K, V>> ofMap(Class<K> keyType, Class<V> valueType) {
        return of(JavaType.forParametrizedClass(Map.class, keyType, valueType));
    }

    /**
     * Creates a {@link Bindable} instance representing a {@link List} with the specified element type.
     *
     * @param rawType the class type of the list elements
     * @param <V>     the type of the elements in the list
     * @return a {@link Bindable} representing a list of the given element type
     */
    public static <V> Bindable<List<V>> ofList(Class<V> rawType) {
        return of(JavaType.forParametrizedClass(List.class, rawType));
    }

    /**
     * Creates a {@link Bindable} instance representing a {@link Set} with the specified element type.
     *
     * @param rawType the class type of the set elements
     * @param <V>     the type of the elements in the set
     * @return a {@link Bindable} representing a set of the given element type
     */
    public static <V> Bindable<Set<V>> ofSet(Class<V> rawType) {
        return of(JavaType.forParametrizedClass(Set.class, rawType));
    }


    /**
     * Creates a new {@link Bindable} instance from a {@link JavaType}.
     *
     * @param type the {@link JavaType} to wrap
     * @param <T>  the expected type
     * @return a new {@link Bindable} instance
     */
    public static <T> Bindable<T> of(JavaType type) {
        return new Bindable<>(type);
    }

    /**
     * Creates a new {@link Bindable} instance from a {@link Class}.
     *
     * @param type the class type to wrap
     * @param <T>  the expected type
     * @return a new {@link Bindable} instance
     */
    public static <T> Bindable<T> of(Class<T> type) {
        return of(JavaType.forClass(type));
    }

    /**
     * Creates a {@link Bindable} instance wrapping an existing object.
     *
     * @param instance the instance to wrap
     * @param <T>      the expected type
     * @return a new {@link Bindable} instance with the provided object as its value
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Bindable<T> ofInstance(T instance) {
        Objects.requireNonNull(instance, "DirectAccess must not be NULL");
        return Bindable.of((Class<T>) instance.getClass()).withInstance(instance);
    }

    /**
     * Retrieves the {@link Supplier} responsible for providing the value.
     *
     * @return the value supplier
     */
    public Supplier<T> getValue() {
        return value;
    }

    /**
     * Retrieves the {@link JavaType} associated with this bindable value.
     *
     * @return the type representation
     */
    public JavaType getType() {
        return type;
    }

    /**
     * Retrieves a {@link TypeInformation} for the underlying {@link JavaType}.
     * <p>
     * This allows for convenient type analysis and classification.
     * </p>
     *
     * @return a {@link TypeInformation} representing this bindable's type
     */
    public TypeInformation getTypeInformation() {
        return TypeInformation.forJavaType(type);
    }

    /**
     * Creates a new {@link Bindable} instance with the specified instance as its value.
     *
     * @param instance the instance to wrap
     * @return a new {@link Bindable} with the same type but the provided instance as value
     */
    public Bindable<T> withInstance(T instance) {
        return withSuppliedInstance(() -> instance);
    }

    /**
     * Creates a new {@link Bindable} instance with the specified instance as its value.
     *
     * @param supplier the supplier with instance
     * @return a new {@link Bindable} with the same type but the provided instance as value
     */
    public Bindable<T> withSuppliedInstance(Supplier<T> supplier) {
        return new Bindable<>(this.type, supplier);
    }

    @Override
    public String toString() {
        return "Bindable(Type: %s)".formatted(type);
    }

}
