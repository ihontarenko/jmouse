package org.jmouse.core.observer;

public interface EventListener<T> {

    String name();

    void update(Event<T> event);

    Class<?> applicableType();

    boolean supports(Class<?> actualType);

}
