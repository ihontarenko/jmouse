package org.jmouse.web.factories;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.context.BeanConditionExpression;
import org.jmouse.core.context.ContextScope;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.validator.*;
import org.jmouse.validator.jsr380.Jsr380Support;
import org.jmouse.web.binding.*;
import org.jmouse.context.BeanConditionExists;

import java.util.List;

@BeanFactories
public class WebParametersDataBinderConfiguration {

    @Bean("parametersDataBinder")
    @BeanConditionExpression("10 % 2 == 0")
    @BeanConditionExists(value = "parametersDataBinder", message = "parameters data binder already registered in client code")
    public ParametersDataBinder parametersDataBinder() {
        ValidatorRegistry registry = new DefaultValidatorRegistry();

        Jsr380Support.registerInto(registry);

        ValidationProcessor validationProcessor = ValidationProcessors.builder()
                .validatorRegistry(registry)
                .validationPolicy(ValidationPolicy.COLLECT_ALL)
                .build();

        ContextScope<BindingContext> bindingScope = new ContextScope<>();
        ErrorsFactory errorsFactory = new DefaultErrorsFactory();

        BindingMappingPlugin bindingPlugin = new BindingMappingPlugin(
                bindingScope,
                List.of(
                        new FailureToErrorsProcessor(new DefaultMappingFailureTranslator()),
                        new ValidationBindingProcessor(validationProcessor)
                )
        );

        Mapper mapper = Mappers.builder()
                .config(MappingConfig.builder()
                                .plugins(List.of(bindingPlugin))
                                .build())
                .build();

        return new ParametersDataBinder(
                mapper,
                errorsFactory,
                bindingScope
        );
    }

    @Bean
    public BindingMappingPlugin bindingMappingPlugin() {
        return null;
    }

}
