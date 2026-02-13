package org.jmouse.web.binding;

import org.jmouse.core.context.ContextScope;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.validator.*;

public final class ParametersDataBinder {

    private final Mapper                       mapper;
    private final ErrorsFactory                errorsFactory;
    private final ContextScope<BindingContext> scope;

    public ParametersDataBinder(
            Mapper mapper,
            ErrorsFactory errorsFactory,
            ContextScope<BindingContext> scope
    ) {
        this.mapper = mapper;
        this.errorsFactory = errorsFactory;
        this.scope = scope;
    }

    public <T> BindingResult<T> bind(Object source, Class<T> targetType, String objectName, ValidationHints hints) {
        Errors         errors  = errorsFactory.create(null, objectName);
        BindingContext context = new BindingContext(errors, objectName, hints);
        T              target;

        try (ContextScope.ScopeToken ignored = scope.open(context)) {
            target = mapper.map(source, targetType);
        }

        return new DefaultBindingResult<>(target, errors);
    }

    public <T> BindingResult<T> bind(Object source, Class<T> targetType, String objectName) {
        return bind(source, targetType, objectName, ValidationHints.empty());
    }
}
