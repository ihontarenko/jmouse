package org.jmouse.core.bind.introspection.internal;

public class AbstractDataContainer<T> implements DataContainer<T> {

    private T      target;
    private String name;

    public AbstractDataContainer(T target) {
        this.target = target;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public T getTarget() {
        return this.target;
    }

    @Override
    public void setTarget(T target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "DataContainer: '%s'[%s]".formatted(name, target);
    }
}
