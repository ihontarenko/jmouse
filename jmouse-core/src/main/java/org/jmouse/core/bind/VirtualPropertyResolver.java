package org.jmouse.core.bind;

@FunctionalInterface
public interface VirtualPropertyResolver<T> {

    Object resolve(T instance, String name);

}
