package org.jmouse.pipeline;

import org.jmouse.pipeline.definition.model.ParameterDefinition;
import org.jmouse.pipeline.definition.model.ProcessorDefinition;
import org.jmouse.common.support.resolver.Resolver;
import org.jmouse.common.support.resolver.Resolvers;
import org.jmouse.common.support.resolver.ResolversFactory;
import org.jmouse.core.Verify;
import org.jmouse.core.reflection.Reflections;

import java.util.List;

import static org.jmouse.common.support.resolver.Resolvers.valueOf;

public class PipelineProcessorFactory {

    private final ResolversFactory resolversFactory;

    public PipelineProcessorFactory() {
        this.resolversFactory = new ResolversFactory();
    }

    public PipelineProcessor createProcessor(ProcessorDefinition definition) {
        Verify.nonNull(definition, "processorDefinition");
        Verify.nonNull(definition.className(), "processorDefinition.className");
        Verify.state(!definition.className().isBlank(), "processorDefinition.className must be non-blank");

        try {
            Class<?>                  processorClass = Class.forName(definition.className());
            List<ParameterDefinition> parameters     = definition.parameters();
            PipelineProcessor         processor      = (PipelineProcessor) Reflections.instantiate(
                    processorClass.getDeclaredConstructor());

            if (parameters != null && !parameters.isEmpty()) {
                for (ParameterDefinition param : parameters) {
                    Object resolved = resolveParameter(param);
                    Reflections.setFieldValue(processor, param.name(), resolved);
                }
            }

            return processor;
        } catch (Exception e) {
            throw new ProcessorInstantiationException(
                    "Error creating processor: '%s'".formatted(definition.className()), e);
        }
    }

    private Object resolveParameter(ParameterDefinition parameter) {
        Verify.nonNull(parameter, "parameter");
        Verify.nonNull(parameter.name(), "parameter.name");

        String resolverName = parameter.resolver();
        if (resolverName == null || resolverName.isBlank()) {
            return parameter.value();
        }

        Resolvers resolverType = valueOf(resolverName.toUpperCase());
        Resolver resolver = resolversFactory.createResolver(resolverType);

        Object value = resolver.resolve(parameter.value(), null);

        // value = convertIfNeeded(value, parameter.converter());

        return value;
    }
}
