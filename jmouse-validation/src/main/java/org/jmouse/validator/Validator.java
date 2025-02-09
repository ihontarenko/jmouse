package org.jmouse.validator;

public interface Validator {
    <T> Set<ConstraintViolation> validate(T object);
}
