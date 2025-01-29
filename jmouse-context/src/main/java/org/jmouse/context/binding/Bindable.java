package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;

import java.util.function.Supplier;

final public class Bindable<T> {

    private final JavaType    type;
    private final Supplier<T> value;

    public Bindable(JavaType type, Supplier<T> value) {
        this.type = type;
        this.value = value;
    }

    public Bindable(JavaType type) {
        this(type, () -> null);
    }

    public Bindable(Class<T> rawType) {
        this(JavaType.forClass(rawType));
    }

    public static <T> Bindable<T> of(JavaType type) {
        return new Bindable<>(type);
    }

    public static <T> Bindable<T> of(Class<?> type) {
        return new Bindable<>(JavaType.forClass(type));
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Bindable<T> ofInstance(T instance) {
        return (Bindable<T>) of(instance.getClass()).withInstance(instance);
    }

    public Supplier<T> getValue() {
        return value;
    }

    public JavaType getType() {
        return type;
    }

    public Bindable<T> withInstance(T instance) {
        return new Bindable<>(this.type, () -> instance);
    }

}
