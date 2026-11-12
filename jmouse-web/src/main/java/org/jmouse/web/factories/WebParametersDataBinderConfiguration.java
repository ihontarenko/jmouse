package org.jmouse.web.factories;

import java.util.List;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.context.BeanIfNameAbsent;
import org.jmouse.core.context.ContextScope;
import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.config.MappingConfig;
import org.jmouse.validator.*;
import org.jmouse.validator.jsr380.Jsr380Support;
import org.jmouse.web.binding.*;

/** Shared validation and one binding scope/plugin, composed explicitly into the form-model binder. */
@BeanFactories
public class WebParametersDataBinderConfiguration {

    public static final String VALIDATION_PROCESSOR = "webValidationProcessor";
    public static final String BINDING_SCOPE = "webBindingScope";

    @Bean(VALIDATION_PROCESSOR)
    @BeanIfNameAbsent(VALIDATION_PROCESSOR)
    public ValidationProcessor webValidationProcessor() {
        ValidatorRegistry registry = new DefaultValidatorRegistry();
        Jsr380Support.registerInto(registry);
        return ValidationProcessors.builder()
            .validationPolicy(ValidationPolicy.COLLECT_ALL)
            .validatorRegistry(registry)
            .build();
    }

    @Bean(BINDING_SCOPE)
    public ContextScope<BindingContext> bindingScope() {
        return new ContextScope<>();
    }

    @Bean("bindingMappingPlugin")
    public BindingMappingPlugin bindingMappingPlugin(
        @Qualifier(BINDING_SCOPE) ContextScope<BindingContext> scope,
        @Qualifier(VALIDATION_PROCESSOR) ValidationProcessor validation
    ) {
        return new BindingMappingPlugin(
            scope,
            List.of(
                new FailureToErrorsProcessor(new DefaultMappingFailureTranslator()),
                new ValidationBindingProcessor(validation)
            )
        );
    }

    @Bean("parametersDataBinder")
    @BeanIfNameAbsent("parametersDataBinder")
    public ParametersDataBinder parametersDataBinder(
        @Qualifier(BINDING_SCOPE) ContextScope<BindingContext> scope,
        BindingMappingPlugin plugin
    ) {
        Mapper mapper = Mappers.builder()
            .config(MappingConfig.builder().plugins(List.of(plugin)).build())
            .build();
        return new ParametersDataBinder(mapper, new DefaultErrorsFactory(), scope);
    }

    /** Retains direct programmatic composition without requiring a container. */
    public ParametersDataBinder parametersDataBinder() {
        ContextScope<BindingContext> scope = bindingScope();
        return parametersDataBinder(scope, bindingMappingPlugin(scope, webValidationProcessor()));
    }
}
