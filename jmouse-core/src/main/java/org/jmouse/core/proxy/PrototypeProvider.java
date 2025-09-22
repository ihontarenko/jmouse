package org.jmouse.core.proxy;

import java.util.function.Supplier;

public final class PrototypeProvider<T> implements InstanceProvider<T> {

    private final Supplier<T> supplier;

    public PrototypeProvider(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        return supplier.get();
    }

    @Override
    public String toString() {
        return "InstanceProvider[PROTOTYPE]";
    }

}
