package org.jmouse.web.binding;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.validator.ErrorsFactory;
import org.jmouse.validator.ValidationProcessor;

public final class ParametersDataBinderFactory {

    private final ParametersDataBinder binder;

    public ParametersDataBinderFactory(ParametersDataBinder binder) {
        this.binder = binder;
    }

    public static ParametersDataBinderFactory defaults(Mapper mapper, ErrorsFactory errorsFactory, ValidationProcessor validationProcessor) {
        ErrorsScope          errorsScope = new ErrorsScope();
        BindingMappingPlugin plugin      = new BindingMappingPlugin(
                errorsScope::get, new DefaultMappingFailureTranslator());
        ParametersDataBinder binder      = new ParametersDataBinder(
                mapper, errorsFactory, errorsScope, validationProcessor);
        return new ParametersDataBinderFactory(binder);
    }

    public ParametersDataBinder create() {
        return binder;
    }
}
