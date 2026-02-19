package org.jmouse.pipeline.definition.loading;

import org.jmouse.pipeline.definition.model.PipelineDefinition;
import org.jmouse.pipeline.definition.processing.DefinitionPostProcessor;

import java.util.List;

public final class DefinitionLoader {

    private final List<PipelineDefinitionReader> readers;
    private final DefinitionPostProcessor        postProcessor;

    public DefinitionLoader(List<PipelineDefinitionReader> readers, DefinitionPostProcessor postProcessor) {
        this.readers = List.copyOf(readers);
        this.postProcessor = postProcessor;
    }

    public PipelineDefinition load(DefinitionSource source) {
        PipelineDefinitionReader reader = readers.stream()
                .filter(r -> r.supports(source))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No reader supports source: " + source.location()));

        PipelineDefinition pipelineDefinition = reader.read(source);
        return postProcessor.process(pipelineDefinition);
    }
}
