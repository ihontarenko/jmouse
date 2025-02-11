package org.jmouse.validator.old.constraint;

import org.jmouse.validator.old.AbstractValidator;
import org.jmouse.validator.old.Errors;
import org.jmouse.validator.old.ValidationContext;

public class NotNullValidator extends AbstractValidator {

    @Override
    public void validate(Object object, Errors errors, ValidationContext context) {
        if (object == null) {
            errors.rejectValue(context.getPointer(), getMessage());
        }
    }

}
