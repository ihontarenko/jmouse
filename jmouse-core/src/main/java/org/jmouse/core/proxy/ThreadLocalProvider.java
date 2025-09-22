package org.jmouse.core.proxy;

import java.util.function.Supplier;

public class ThreadLocalProvider<T> implements InstanceProvider<T> {

    private final ThreadLocal<T>        threadLocal;
    private final Supplier<? extends T> supplier;

    public ThreadLocalProvider(Supplier<? extends T> supplier) {
        this.supplier = supplier;
        this.threadLocal = ThreadLocal.withInitial(() -> null);
    }


    @Override
    public T get() {
        T value = threadLocal.get();

        if (value == null) {
            value = supplier.get();
            threadLocal.set(value);
        }

        return value;
    }

    public void remove() {
        threadLocal.remove();
    }

    @Override
    public String toString() {
        return "InstanceProvider[THREAD_LOCAL]";
    }

}
