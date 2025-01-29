package org.jmouse.context.binding;

import java.util.function.Consumer;
import java.util.function.Supplier;

final public class BindingResult<T> {

    private final T value;

    public BindingResult(T value) {
        this.value = value;
    }

    public static <T> BindingResult<T> of(T value) {
        return new BindingResult<>(value);
    }

    public T getValue(Supplier<? extends Error> exceptionSupplier) {
        if (!isPresent()) {
            throw exceptionSupplier.get();
        }

        return value;
    }

    public T getValue(T defaultValue) {
        return isPresent() ? value : defaultValue;
    }

    public T getValue() {
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public void ifPresent(Consumer<T> consumer) {
        if (isPresent()) {
            consumer.accept(value);
        }
    }



}
