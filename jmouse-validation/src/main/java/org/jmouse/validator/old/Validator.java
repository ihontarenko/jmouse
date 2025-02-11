package org.jmouse.validator.old;

public interface Validator {

    void validate(Object object, Errors errors, ValidationContext context) throws ValidationException;

    default boolean supports(Object object) {
        return true;
    }

}
