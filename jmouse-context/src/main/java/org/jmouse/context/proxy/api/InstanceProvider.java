package org.jmouse.context.proxy.api;

import java.util.function.Supplier;

@FunctionalInterface
public interface InstanceProvider<T> {

    T get();

    static <T> InstanceProvider<T> singleton(T instance) {
        return new SingletonProvider<>(instance);
    }

    static <T> InstanceProvider<T> prototype(Supplier<T> supplier) {
        return new PrototypeProvider<>(supplier);
    }

    static <T> InstanceProvider<T> threadLocal(Supplier<T> supplier) {
        return new ThreadLocalProvider<>(supplier);
    }

}
