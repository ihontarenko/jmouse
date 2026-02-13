package org.jmouse.validator.jsr380;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.jmouse.validator.Errors;
import org.jmouse.validator.SmartValidator;
import org.jmouse.validator.ValidationHints;

import java.util.Set;

public final class Jsr380ValidatorAdapter implements SmartValidator {

    private final Validator             validator;
    private final Jsr380ViolationMapper violationMapper;

    public Jsr380ValidatorAdapter(Validator validator) {
        this(validator, new DefaultJsr380ViolationMapper());
    }

    public Jsr380ValidatorAdapter(Validator validator, Jsr380ViolationMapper violationMapper) {
        this.validator = validator;
        this.violationMapper = violationMapper;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public void validate(Object target, Errors errors, ValidationHints hints) {
        if (target == null) {
            return;
        }

        Class<?>[]                       groups     = toGroups(hints);
        Set<ConstraintViolation<Object>> violations = validator.validate(target, groups);

        for (ConstraintViolation<?> violation : violations) {
            violationMapper.apply(violation, errors);
        }
    }

    private static Class<?>[] toGroups(ValidationHints hints) {
        if (hints == null) {
            return new Class<?>[0];
        }

        ValidationGroups groups = hints.find(ValidationGroups.class);

        if (groups != null && groups.groups() != null) {
            return groups.groups();
        }

        return hints.values().stream()
                .filter(value -> value instanceof Class<?>)
                .map(type -> (Class<?>) type)
                .toArray(Class[]::new);
    }
}
