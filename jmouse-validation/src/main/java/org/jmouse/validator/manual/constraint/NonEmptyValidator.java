package org.jmouse.validator.manual.constraint;

import org.jmouse.validator.manual.AbstractValidator;
import org.jmouse.validator.manual.Errors;
import org.jmouse.validator.manual.ValidationContext;

public class NonEmptyValidator extends AbstractValidator {

    @Override
    public void validate(Object object, Errors errors, ValidationContext context) {
        if (object == null || (object instanceof String string && string.isBlank())) {
            errors.rejectValue(context.getPointer(), getMessage());
        }
    }

}
