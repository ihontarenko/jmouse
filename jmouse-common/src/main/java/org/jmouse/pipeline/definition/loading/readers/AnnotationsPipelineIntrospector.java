package org.jmouse.pipeline.definition.loading.readers;

import org.jmouse.pipeline.PipelineProcessor;
import org.jmouse.pipeline.definition.annotations.*;
import org.jmouse.pipeline.definition.dsl.PipelineBuilder;
import org.jmouse.pipeline.definition.dsl.ChainBuilder;
import org.jmouse.pipeline.definition.dsl.LinkBuilder;
import org.jmouse.core.Verify;
import org.jmouse.core.reflection.ClassFinder;

import java.util.Collection;

import static org.jmouse.util.StringHelper.blankToNull;

public final class AnnotationsPipelineIntrospector {

    /**
     * Contribute definition into the provided {@link PipelineBuilder}, scanning classes starting from base classes.
     */
    public void contribute(PipelineBuilder pipeline, Class<?>... baseClasses) {
        Verify.nonNull(pipeline, "pipeline");
        Verify.nonNull(baseClasses, "baseClasses");

        Collection<Class<?>> links = ClassFinder.findAnnotatedClasses(PipelineLink.class, baseClasses);

        for (Class<?> linkClass : links) {
            PipelineLink link = linkClass.getAnnotation(PipelineLink.class);

            String chainName = link.chain();
            String linkName  = link.name();

            pipeline.chain(chainName, c -> applyLink(c, linkClass, linkName, link.initial()));
        }
    }

    @SuppressWarnings("unchecked")
    private void applyLink(ChainBuilder chainBuilder, Class<?> processorClass, String linkName, boolean initial) {
        if (initial) {
            chainBuilder.initial(linkName);
        }

        LinkBuilder linkBuilder = chainBuilder.link(linkName);

        if (!PipelineProcessor.class.isAssignableFrom(processorClass)) {
            throw new IllegalArgumentException("Class annotated with @PipelineLink must implement PipelineProcessor: "
                    + processorClass.getName());
        }

        linkBuilder.processor((Class<? extends PipelineProcessor>) processorClass);

        // transitions
        for (OnReturn onReturn : processorClass.getAnnotationsByType(OnReturn.class)) {
            linkBuilder.onReturn(onReturn.code(), onReturn.link());
        }

        // fallback
        Fallback fallback = processorClass.getAnnotation(Fallback.class);
        if (fallback != null && fallback.link() != null && !fallback.link().isBlank()) {
            linkBuilder.fallback(fallback.link());
        }

        // config
        for (Config config : processorClass.getAnnotationsByType(Config.class)) {
            linkBuilder.config(config.key(), config.value());
        }

        // parameters
        for (Parameter parameter : processorClass.getAnnotationsByType(Parameter.class)) {
            linkBuilder.param(
                    parameter.name(),
                    parameter.value(),
                    blankToNull(parameter.resolver()),
                    blankToNull(parameter.converter())
            );
        }
    }
}
