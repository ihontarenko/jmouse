package org.jmouse.core.bind.introspection.internal;

public interface DataContainer<T> {

    String getName();

    void setName(String name);

    T getTarget();

    void setTarget(T target);

}
