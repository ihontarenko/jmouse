package org.jmouse.validator;

public final class ValidationProcessors {

    private ValidationProcessors() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ValidatorRegistry  validatorRegistry;
        private ValidatorProvider  validatorProvider;
        private ErrorsFactory      errorsFactory;
        private ValidationPolicy   validationPolicy;

        private Builder() {
        }

        public Builder validatorRegistry(ValidatorRegistry registry) {
            this.validatorRegistry = registry;
            return this;
        }

        public Builder validatorProvider(ValidatorProvider provider) {
            this.validatorProvider = provider;
            return this;
        }

        public Builder errorsFactory(ErrorsFactory factory) {
            this.errorsFactory = factory;
            return this;
        }

        public Builder validationPolicy(ValidationPolicy policy) {
            this.validationPolicy = policy;
            return this;
        }

        public ValidationProcessor build() {
            ValidatorRegistry registry = validatorRegistry;
            ValidatorProvider provider = validatorProvider;

            if (registry == null && provider == null) {
                DefaultValidatorRegistry defaultRegistry = new DefaultValidatorRegistry();
                registry = defaultRegistry;
                provider = defaultRegistry;
            } else if (registry == null) {
                registry = toRegistry(provider);
            } else if (provider == null) {
                provider = toProvider(registry);
            }

            ErrorsFactory factory = errorsFactory == null ? new DefaultErrorsFactory() : errorsFactory;
            ValidationPolicy policy = validationPolicy == null ? ValidationPolicy.COLLECT_ALL : validationPolicy;

            return new DefaultValidationProcessor(registry, factory, policy);
        }

        private static ValidatorRegistry toRegistry(ValidatorProvider provider) {
            return new ValidatorRegistry() {
                @Override
                public void register(Validator validator) {
                    throw new UnsupportedOperationException(
                            "This ValidationProcessor was configured with ValidatorProvider only; registry is read-only.");
                }

                @Override
                public void validate(Object target, Errors errors) {
                    if (target == null || errors == null) {
                        return;
                    }
                    Class<?> type = target.getClass();
                    for (Validator validator : provider.getValidators()) {
                        if (validator.supports(type)) {
                            validator.validate(target, errors);
                        }
                    }
                }
            };
        }

        private static ValidatorProvider toProvider(ValidatorRegistry registry) {
            if (registry instanceof ValidatorProvider provider) {
                return provider;
            }
            throw new IllegalStateException(
                    "ValidatorRegistry does not provide validators. " +
                            "Either implement ValidatorProvider or pass validatorProvider(...) explicitly.");
        }
    }
}
