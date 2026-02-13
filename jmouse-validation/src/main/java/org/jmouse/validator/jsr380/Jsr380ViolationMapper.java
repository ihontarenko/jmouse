package org.jmouse.validator.jsr380;

import jakarta.validation.ConstraintViolation;
import org.jmouse.validator.Errors;

public interface Jsr380ViolationMapper {
    void apply(ConstraintViolation<?> violation, Errors errors);
}
