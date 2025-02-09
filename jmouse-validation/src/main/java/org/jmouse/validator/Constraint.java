package org.jmouse.validator;

public interface Constraint<T> {

    boolean isValid(T value);

    String getMessage();

}
