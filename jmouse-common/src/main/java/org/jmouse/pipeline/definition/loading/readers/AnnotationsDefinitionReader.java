package org.jmouse.pipeline.definition.loading.readers;

import org.jmouse.pipeline.definition.dsl.PipelineDefinitions;
import org.jmouse.pipeline.definition.loading.DefinitionSource;
import org.jmouse.pipeline.definition.loading.PipelineDefinitionReader;
import org.jmouse.pipeline.definition.model.PipelineDefinition;

public final class AnnotationsDefinitionReader implements PipelineDefinitionReader {

    private final AnnotationsPipelineIntrospector introspector;

    public AnnotationsDefinitionReader(AnnotationsPipelineIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public boolean supports(DefinitionSource source) {
        return "annotations".equals(source.attributes().get("type"));
    }

    @Override
    public PipelineDefinition read(DefinitionSource source) {
        String     pipelineName = (String) source.attributes().get("pipelineName");
        Class<?>[] baseClasses  = (Class<?>[]) source.attributes().get("baseClasses");

        return PipelineDefinitions.pipeline(pipelineName, p ->
                introspector.contribute(p, baseClasses)
        );
    }
}
