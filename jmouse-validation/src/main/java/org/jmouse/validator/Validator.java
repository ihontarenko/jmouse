package org.jmouse.validator;

import java.util.Set;

public interface Validator {
    <T> Set<ConstraintViolation> validate(T object);
}
