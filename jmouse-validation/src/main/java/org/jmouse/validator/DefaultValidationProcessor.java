package org.jmouse.validator;

import java.util.List;

public final class DefaultValidationProcessor implements ValidationProcessor {

    private final ValidatorRegistry registry;
    private final ErrorsFactory     errorsFactory;
    private final ValidationPolicy  policy;

    public DefaultValidationProcessor(ValidatorRegistry registry, ErrorsFactory errorsFactory, ValidationPolicy policy) {
        this.registry = registry;
        this.errorsFactory = errorsFactory == null ? new DefaultErrorsFactory() : errorsFactory;
        this.policy = policy == null ? ValidationPolicy.COLLECT_ALL : policy;
    }

    @Override
    public <T> ValidationResult<T> validate(T target, String objectName) {
        return validate(target, objectName, ValidationHints.empty());
    }

    @Override
    public <T> ValidationResult<T> validate(T target, String objectName, ValidationHints hints) {
        Errors errors = errorsFactory.create(target, objectName);

        doValidate(target, errors, hints);

        if (policy == ValidationPolicy.FAIL_FAST && (errors.hasErrors() || errors.hasGlobalErrors())) {
            throw new ValidationException(errors);
        }

        return new DefaultValidationResult<>(objectName, target, errors);
    }

    private void doValidate(Object target, Errors errors, ValidationHints hints) {
        if (target == null) {
            return;
        }

        if (registry instanceof ValidatorProvider validatorProvider) {
            List<Validator> validators = validatorProvider.getValidators();
            Class<?>        type       = target.getClass();

            for (Validator validator : validators) {
                if (!validator.supports(type)) {
                    continue;
                }

                if (validator instanceof SmartValidator smart) {
                    smart.validate(target, errors, hints);
                } else {
                    validator.validate(target, errors);
                }

                if (policy == ValidationPolicy.FAIL_FAST && (errors.hasErrors() || errors.hasGlobalErrors())) {
                    return;
                }
            }
            return;
        }

        registry.validate(target, errors);
    }
}
