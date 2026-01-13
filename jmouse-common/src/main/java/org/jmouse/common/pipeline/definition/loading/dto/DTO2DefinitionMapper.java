package org.jmouse.common.pipeline.definition.loading.dto;

import org.jmouse.common.pipeline.definition.model.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DTO2DefinitionMapper {

    public PipelineDefinition map(PipelineDTO dto) {
        Map<String, ChainDefinition> chains = new LinkedHashMap<>();
        dto.chains.forEach((k, chainDTO) -> chains.put(chainDTO.name, mapChain(chainDTO)));
        return new PipelineDefinition(dto.name, chains);
    }

    private ChainDefinition mapChain(PipelineDTO.ChainDTO chainDTO) {
        Map<String, LinkDefinition> links = new LinkedHashMap<>();
        chainDTO.links.forEach((k, linkDTO) -> links.put(linkDTO.name, mapLink(linkDTO)));
        return new ChainDefinition(chainDTO.name, chainDTO.initial, links);
    }

    private LinkDefinition mapLink(PipelineDTO.LinkDTO linkDTO) {
        ProcessorDefinition processorDefinition = null;

        if (linkDTO.processor != null) {
            List<ParameterDefinition> parameterDefinitions = linkDTO.processor.parameters == null
                    ? List.of()
                    : linkDTO.processor.parameters.stream().map(parameterDTO -> new ParameterDefinition(
                            parameterDTO.name,
                            parameterDTO.value,
                            parameterDTO.resolver,
                            parameterDTO.converter
                    )).toList();
            processorDefinition = new ProcessorDefinition(linkDTO.processor.className, parameterDefinitions);
        }

        LinkProperties linkProperties = (linkDTO.properties == null)
                ? LinkProperties.empty()
                : new LinkProperties(
                        linkDTO.properties.transitions,
                        linkDTO.properties.configuration,
                        linkDTO.properties.fallback
                );

        return new LinkDefinition(linkDTO.name, processorDefinition, linkProperties);
    }
}
