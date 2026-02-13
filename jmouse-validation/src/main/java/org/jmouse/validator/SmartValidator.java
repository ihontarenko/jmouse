package org.jmouse.validator;

public interface SmartValidator extends Validator {

    /**
     * Validate with optional hints (groups, modes, partial updates, etc).
     */
    void validate(Object target, Errors errors, ValidationHints hints);

    @Override
    default void validate(Object object, Errors errors) {
        validate(object, errors, ValidationHints.empty());
    }
}
