package org.jmouse.validator;

public interface SmartValidator extends Validator {

    void validate(Object target, Errors errors, ValidationHints hints);

    @Override
    default void validate(Object object, Errors errors) {
        validate(object, errors, ValidationHints.empty());
    }
}
