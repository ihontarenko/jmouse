package org.jmouse.pipeline.definition.processing;

import org.jmouse.common.pipeline.definition.model.*;
import org.jmouse.pipeline.definition.model.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.jmouse.util.StringHelper.trim;

public final class NormalizeProcessor implements DefinitionPostProcessor {

    @Override
    public PipelineDefinition process(PipelineDefinition def) {
        if (def == null) {
            throw new DefinitionProcessingException("PipelineDefinition must be non-null");
        }

        Map<String, ChainDefinition> chains = new LinkedHashMap<>();
        for (ChainDefinition c : def.chains().values()) {
            chains.put(c.name(), normalizeChain(c));
        }
        return new PipelineDefinition(def.name(), chains);
    }

    private ChainDefinition normalizeChain(ChainDefinition chainDefinition) {
        Map<String, LinkDefinition> links = new LinkedHashMap<>();

        for (LinkDefinition linkDefinition : chainDefinition.links().values()) {
            links.put(linkDefinition.name(), normalizeLink(linkDefinition));
        }

        return new ChainDefinition(trim(chainDefinition.name()), trim(chainDefinition.initial()), links);
    }

    private LinkDefinition normalizeLink(LinkDefinition linkDefinition) {
        ProcessorDefinition processorDefinition = linkDefinition.processor();
        ProcessorDefinition definition          = (processorDefinition == null)
                ? null
                : new ProcessorDefinition(trim(processorDefinition.className()), processorDefinition.parameters());
        LinkProperties properties          = (linkDefinition.properties() == null
                ? LinkProperties.empty()
                : linkDefinition.properties());

        Map<String, String> transitions = new LinkedHashMap<>();
        properties.transitions().forEach((key, value) -> transitions.put(trim(key), trim(value)));

        Map<String, String> config = new LinkedHashMap<>();
        properties.configuration().forEach((key, value) -> config.put(trim(key), trim(value)));

        LinkProperties linkProperties = new LinkProperties(transitions, config, trim(properties.fallback()));

        return new LinkDefinition(trim(linkDefinition.name()), definition, linkProperties);
    }


}
