package org.jmouse.validator;

public interface ValidationResult<T> {

    String objectName();

    T target();

    Errors errors();

    default boolean hasErrors() {
        return errors().hasErrors() || errors().hasGlobalErrors();
    }
}
