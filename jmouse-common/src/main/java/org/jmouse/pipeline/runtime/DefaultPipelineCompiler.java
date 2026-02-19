package org.jmouse.pipeline.runtime;

import org.jmouse.common.pipeline.*;
import org.jmouse.common.pipeline.definition.model.*;
import org.jmouse.core.proxy.ProxyFactory;
import org.jmouse.pipeline.*;
import org.jmouse.pipeline.definition.model.*;

import java.util.HashMap;
import java.util.Map;

public final class DefaultPipelineCompiler implements PipelineCompiler {

    private final PipelineProcessorFactory processorFactory;
    private final ProxyFactory             proxyFactory;

    public DefaultPipelineCompiler(PipelineProcessorFactory processorFactory, ProxyFactory proxyFactory) {
        this.processorFactory = processorFactory;
        this.proxyFactory = proxyFactory;
    }

    @Override
    public PipelineChain compile(PipelineDefinition definition, String chainName) {
        if (definition == null) {
            throw new PipelineCompilationException("PipelineDefinition must be non-null");
        }

        ChainDefinition chainDefinition = definition.chains().get(chainName);

        if (chainDefinition == null) {
            throw new PipelineCompilationException("No chain definition found: " + chainName);
        }

        Map<String, PipelineProcessor>   processors = new HashMap<>();
        Map<String, ProcessorProperties> properties = new HashMap<>();

        for (LinkDefinition linkDefinition : chainDefinition.links().values()) {
            String              linkName            = linkDefinition.name();
            ProcessorProperties processorProperties = mapProperties(linkDefinition.properties());
            PipelineProcessor   processor           = createProcessor(linkDefinition);

            processors.put(linkName, proxyFactory.createProxy(processor));
            properties.put(linkName, processorProperties);
        }

        PipelineChain chain = new PipelineProcessorChain(chainDefinition.initial(), processors, properties);

        return proxyFactory.createProxy(chain);
    }

    private PipelineProcessor createProcessor(LinkDefinition linkDef) {
        ProcessorDefinition processorDefinition = linkDef.processor();

        if (processorDefinition == null || processorDefinition.className() == null || processorDefinition.className().isBlank()) {
            throw new PipelineCompilationException("Link '%s' has no processor.className".formatted(linkDef.name()));
        }

        return processorFactory.createProcessor(processorDefinition);
    }

    private ProcessorProperties mapProperties(LinkProperties linkProperties) {
        if (linkProperties == null) {
            return new ProcessorProperties(new HashMap<>(), new HashMap<>(), null);
        }

        Map<String, String> transitions = (linkProperties.transitions() == null
                ? new HashMap<>() : new HashMap<>(linkProperties.transitions()));
        Map<String, String> config      = (linkProperties.configuration() == null
                ? new HashMap<>() : new HashMap<>(linkProperties.configuration()));
        String              fallback    = linkProperties.fallback();

        return new ProcessorProperties(transitions, config, fallback);
    }
}