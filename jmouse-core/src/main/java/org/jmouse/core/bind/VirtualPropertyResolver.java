package org.jmouse.core.bind;

public interface VirtualPropertyResolver<T> {
    Object resolve(T instance, String name);
}
