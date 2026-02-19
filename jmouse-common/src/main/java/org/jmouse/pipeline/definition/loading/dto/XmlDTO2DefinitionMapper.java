package org.jmouse.pipeline.definition.loading.dto;

import org.jmouse.pipeline.definition.loading.dto.xml.XmlRootDTO;
import org.jmouse.pipeline.definition.model.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XmlDTO2DefinitionMapper {

    public PipelineDefinition map(XmlRootDTO xmlRootDTO) {
        Map<String, ChainDefinition> chains = new LinkedHashMap<>();
        xmlRootDTO.chains().values().forEach(chain -> chains.put(chain.name(), mapChain(chain)));
        return new PipelineDefinition(xmlRootDTO.name(), chains);
    }

    private ChainDefinition mapChain(XmlRootDTO.Chain chain) {
        Map<String, LinkDefinition> links = new LinkedHashMap<>();
        chain.links().values().forEach(link -> links.put(link.name(), mapLink(link)));
        return new ChainDefinition(chain.name(), chain.initial(), links);
    }

    private LinkDefinition mapLink(XmlRootDTO.Link link) {
        ProcessorDefinition processor = null;
        if (link.processor() != null) {
            List<ParameterDefinition> parameterDefinitions = link.processor().parameters() == null
                    ? List.of()
                    : link.processor().parameters().stream().map(parameter -> new ParameterDefinition(
                            parameter.name(),
                            parameter.value(),
                            parameter.resolver(),
                            parameter.converter()
                    )).toList();

            processor = new ProcessorDefinition(link.processor().className(), parameterDefinitions);
        }

        LinkProperties linkProperties = LinkProperties.empty();

        if (link.properties() != null) {
            Map<String, String> transitions = link.properties().transitions() == null
                    ? Map.of()
                    : link.properties().transitions();

            Map<String, String> configuration = link.properties().configuration() == null
                    ? Map.of()
                    : link.properties().configuration();

            String fallback = (link.properties().fallback() == null ? null : link.properties().fallback().link());
            linkProperties = new LinkProperties(transitions, configuration, fallback);
        }

        return new LinkDefinition(link.name(), processor, linkProperties);
    }

}
