package org.jmouse.validator;

public interface ConstraintViolation {

    Object getInvalidValue();

    String getMessage();

}
