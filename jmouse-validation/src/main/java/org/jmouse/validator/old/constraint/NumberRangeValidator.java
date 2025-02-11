package org.jmouse.validator.old.constraint;

import org.jmouse.validator.old.AbstractValidator;
import org.jmouse.validator.old.Errors;
import org.jmouse.validator.old.ValidationContext;

public class NumberRangeValidator extends AbstractValidator {

    private Number min;
    private Number max;

    @Override
    public void validate(Object object, Errors errors, ValidationContext context) {
        if (object instanceof Number number && (number.doubleValue() < min.intValue() || number.doubleValue() > max.intValue())) {
            errors.rejectValue(context.getPointer(), getMessage());
        }
    }

    @Override
    public String toString() {
        return "%s : [min=%d, max='%d']".formatted(super.toString(), min.intValue(), max.intValue());
    }

}
