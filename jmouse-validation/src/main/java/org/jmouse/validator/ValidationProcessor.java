package org.jmouse.validator;

public interface ValidationProcessor {

    <T> ValidationResult<T> validate(T target, String objectName);

    <T> ValidationResult<T> validate(T target, String objectName, ValidationHints hints);
}
