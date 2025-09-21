package org.jmouse.core.proxy2.api;

@FunctionalInterface
public interface TargetProvider<T> {

    T get();

    static <T> TargetProvider<T> singleton(T instance) {
        return () -> instance;
    }

}
