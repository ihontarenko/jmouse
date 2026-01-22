package org.jmouse.core.bind;

import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Represents a bindable value with an associated {@link InferredType}.
 * <p>
 * This class allows deferred retrieval of values via a {@link Supplier}, making it useful
 * for dynamic type binding and lazy initialization.
 * </p>
 *
 * @param <T> the type of the bindable value
 */
public final class TypedValue<T> {

    private final InferredType type;
    private final Supplier<T>  value;

    /**
     * Creates a new {@link TypedValue} instance with a specified type and value supplier.
     *
     * @param type  the {@link InferredType} representing the type of the value
     * @param value a {@link Supplier} providing the value
     */
    public TypedValue(InferredType type, Supplier<T> value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a new {@link TypedValue} instance with a specified type and a default {@code null} supplier.
     *
     * @param type the {@link InferredType} representing the type of the value
     */
    public TypedValue(InferredType type) {
        this(type, () -> null);
    }

    /**
     * Creates a new {@link TypedValue} instance for a given raw class type.
     *
     * @param rawType the class type to wrap
     */
    public TypedValue(Class<T> rawType) {
        this(InferredType.forClass(rawType));
    }

    /**
     * Creates a {@link TypedValue} instance representing a {@link Map} with the specified key and value types.
     *
     * @param keyType   the class type of the map keys
     * @param valueType the class type of the map values
     * @param <K>       the type of the keys in the map
     * @param <V>       the type of the values in the map
     * @return a {@link TypedValue} representing a map with the given key-value types
     */
    public static <K, V> TypedValue<Map<K, V>> ofMap(Class<K> keyType, Class<V> valueType) {
        return of(InferredType.forParametrizedClass(Map.class, keyType, valueType));
    }

    /**
     * Creates a {@link TypedValue} instance representing a {@link List} with the specified element type.
     *
     * @param rawType the class type of the list elements
     * @param <V>     the type of the elements in the list
     * @return a {@link TypedValue} representing a list of the given element type
     */
    public static <V> TypedValue<List<V>> ofList(Class<V> rawType) {
        return of(InferredType.forParametrizedClass(List.class, rawType));
    }

    /**
     * Creates a {@link TypedValue} instance representing a {@link Set} with the specified element type.
     *
     * @param rawType the class type of the set elements
     * @param <V>     the type of the elements in the set
     * @return a {@link TypedValue} representing a set of the given element type
     */
    public static <V> TypedValue<Set<V>> ofSet(Class<V> rawType) {
        return of(InferredType.forParametrizedClass(Set.class, rawType));
    }


    /**
     * Creates a new {@link TypedValue} instance from a {@link InferredType}.
     *
     * @param type the {@link InferredType} to wrap
     * @param <T>  the expected type
     * @return a new {@link TypedValue} instance
     */
    public static <T> TypedValue<T> of(InferredType type) {
        return new TypedValue<>(type);
    }

    /**
     * Creates a new {@link TypedValue} instance from a {@link Class}.
     *
     * @param type the class type to wrap
     * @param <T>  the expected type
     * @return a new {@link TypedValue} instance
     */
    public static <T> TypedValue<T> of(Class<T> type) {
        return of(InferredType.forClass(type));
    }

    /**
     * Creates a {@link TypedValue} instance wrapping an existing object.
     *
     * @param instance the instance to wrap
     * @param <T>      the expected type
     * @return a new {@link TypedValue} instance with the provided object as its value
     */
    @SuppressWarnings({"unchecked"})
    public static <T> TypedValue<T> ofInstance(T instance) {
        Objects.requireNonNull(instance, "DirectAccess must not be NULL");
        return TypedValue.of((Class<T>) instance.getClass()).withInstance(instance);
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
     * Retrieves the {@link InferredType} associated with this bindable value.
     *
     * @return the type representation
     */
    public InferredType getType() {
        return type;
    }

    /**
     * Retrieves a {@link TypeInformation} for the underlying {@link InferredType}.
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
     * Creates a new {@link TypedValue} instance with the specified instance as its value.
     *
     * @param instance the instance to wrap
     * @return a new {@link TypedValue} with the same type but the provided instance as value
     */
    public TypedValue<T> withInstance(T instance) {
        return withSuppliedInstance(() -> instance);
    }

    /**
     * Creates a new {@link TypedValue} instance with the specified instance as its value.
     *
     * @param supplier the supplier with instance
     * @return a new {@link TypedValue} with the same type but the provided instance as value
     */
    public TypedValue<T> withSuppliedInstance(Supplier<T> supplier) {
        return new TypedValue<>(this.type, supplier);
    }

    @Override
    public String toString() {
        return "Bindable(Type: %s)".formatted(type);
    }

}
