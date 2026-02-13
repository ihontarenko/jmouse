package org.jmouse.web.binding;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.validator.*;

public final class ParametersDataBinder {

    private final Mapper              mapper;
    private final ErrorsFactory       errorsFactory;
    private final BindingContextScope bindingScope;

    public ParametersDataBinder(
            Mapper mapper,
            ErrorsFactory errorsFactory,
            BindingContextScope bindingScope
    ) {
        this.mapper = mapper;
        this.errorsFactory = errorsFactory;
        this.bindingScope = bindingScope;
    }

    public <T> BindingResult<T> bind(Object source, Class<T> targetType, String objectName, ValidationHints hints) {
        Errors errors = errorsFactory.create(null, objectName);

        BindingContext context = new BindingContext(errors, objectName, hints);

        T target;

        try (BindingContextScope.ScopeToken ignored = bindingScope.open(context)) {
            target = mapper.map(source, targetType);
        }

        return new DefaultBindingResult<>(target, errors);
    }

    public <T> BindingResult<T> bind(Object source, Class<T> targetType, String objectName) {
        return bind(source, targetType, objectName, ValidationHints.empty());
    }
}
