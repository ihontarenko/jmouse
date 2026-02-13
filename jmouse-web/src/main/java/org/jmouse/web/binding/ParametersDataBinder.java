package org.jmouse.web.binding;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.validator.*;

public final class ParametersDataBinder {

    private final ValidationProcessor validationProcessor;
    private final Mapper              mapper;
    private final ErrorsFactory       errorsFactory;
    private final ErrorsScope         errorsScope;

    public ParametersDataBinder(
            Mapper mapper,
            ErrorsFactory errorsFactory,
            ErrorsScope errorsScope,
            ValidationProcessor validationProcessor
    ) {
        this.mapper = mapper;
        this.errorsFactory = errorsFactory;
        this.validationProcessor = validationProcessor;
        this.errorsScope = errorsScope;
    }

    public <T> BindingResult<T> bind(Object source, Class<T> targetType, String objectName, ValidationHints hints) {
        Errors errors = errorsFactory.create(null, objectName);
        T      target;

        try (ErrorsScope.ScopeToken ignored = errorsScope.open(errors)) {
            target = mapper.map(source, targetType);
        }

        ValidationResult<T> validation = validationProcessor.validate(target, objectName, hints);

        ErrorsMerging.merge(errors, validation.errors());

        return new DefaultBindingResult<>(target, errors);
    }

    public <T> BindingResult<T> bind(Object source, Class<T> targetType, String objectName) {
        return bind(source, targetType, objectName, ValidationHints.empty());
    }
}
