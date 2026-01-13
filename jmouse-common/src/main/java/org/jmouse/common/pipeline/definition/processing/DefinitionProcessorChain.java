package org.jmouse.common.pipeline.definition.processing;

import org.jmouse.common.pipeline.definition.model.PipelineDefinition;

import java.util.List;

public final class DefinitionProcessorChain implements DefinitionPostProcessor {

    private final List<DefinitionPostProcessor> processors;

    public DefinitionProcessorChain(List<DefinitionPostProcessor> processors) {
        this.processors = List.copyOf(processors);
    }

    @Override
    public PipelineDefinition process(PipelineDefinition definition) {
        PipelineDefinition output = definition;
        for (DefinitionPostProcessor postProcessor : processors) {
            output = postProcessor.process(output);
        }
        return output;
    }
}

