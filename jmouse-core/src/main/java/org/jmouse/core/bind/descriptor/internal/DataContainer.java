package org.jmouse.core.bind.descriptor.internal;

public interface DataContainer<T> {

    String getName();

    void setName(String name);

    T getTarget();

    void setTarget(T target);

}
