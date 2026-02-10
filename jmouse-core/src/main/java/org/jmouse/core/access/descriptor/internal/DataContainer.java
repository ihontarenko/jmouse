package org.jmouse.core.access.descriptor.internal;

public interface DataContainer<T> {

    String getName();

    void setName(String name);

    T getTarget();

    void setTarget(T target);

}
