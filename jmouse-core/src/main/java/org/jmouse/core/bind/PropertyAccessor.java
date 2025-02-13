package org.jmouse.core.bind;

public interface PropertyAccessor<T> {

    void injectValue(T object, Object value);

    Object obtainValue(T object);

}
