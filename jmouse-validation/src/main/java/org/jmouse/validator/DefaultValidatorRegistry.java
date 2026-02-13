package org.jmouse.validator;

import java.util.ArrayList;
import java.util.List;

public final class DefaultValidatorRegistry implements ValidatorRegistry, ValidatorProvider {

    private final List<Validator> validators = new ArrayList<>();

    @Override
    public void register(Validator validator) {
        if (validator == null) {
            return;
        }
        validators.add(validator);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target == null || errors == null) {
            return;
        }

        Class<?> type = target.getClass();

        for (Validator validator : validators) {
            if (validator.supports(type)) {
                validator.validate(target, errors);
            }
        }
    }

    @Override
    public List<Validator> getValidators() {
        return List.copyOf(validators);
    }
}
