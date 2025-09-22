package org.jmouse.core.proxy;

public final class SingletonProvider<T> implements InstanceProvider<T> {

    private final T instance;

    public SingletonProvider(T instance) {
        this.instance = instance;
    }

    @Override
    public T get() {
        return instance;
    }

    @Override
    public String toString() {
        return "InstanceProvider[SINGLETON]";
    }

}
