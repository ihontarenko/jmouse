package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeDescriptor;

import java.util.Objects;
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
        return new Bindable<>(JavaType.forClass(type));
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
        Objects.requireNonNull(instance, "Instance must not be NULL");
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
     * Retrieves a {@link TypeDescriptor} for the underlying {@link JavaType}.
     * <p>
     * This allows for convenient type analysis and classification.
     * </p>
     *
     * @return a {@link TypeDescriptor} representing this bindable's type
     */
    public TypeDescriptor getTypeDescriptor() {
        return TypeDescriptor.forJavaType(type);
    }

    /**
     * Creates a new {@link Bindable} instance with the specified instance as its value.
     *
     * @param instance the instance to wrap
     * @return a new {@link Bindable} with the same type but the provided instance as value
     */
    public Bindable<T> withInstance(T instance) {
        return withInstance(() -> instance);
    }

    /**
     * Creates a new {@link Bindable} instance with the specified instance as its value.
     *
     * @param supplier the supplier with instance
     * @return a new {@link Bindable} with the same type but the provided instance as value
     */
    public Bindable<T>  withInstance(Supplier<T> supplier) {
        return new Bindable<>(this.type, supplier);
    }

    @Override
    public String toString() {
        return "Bindable(Type: %s; Instance: %s)".formatted(type, value.get());
    }

}
