package org.jmouse.common.pipeline;

import org.jmouse.common.pipeline.context.PipelineContext;
import org.jmouse.common.pipeline.definition.PipelineDefinitionException;
import org.jmouse.common.pipeline.definition.RootDefinition;
import org.jmouse.core.proxy.DefaultProxyFactory;
import org.jmouse.core.proxy.ProxyFactory;
import org.jmouse.common.pipeline.definition.DefinitionLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PipelineManager {

    private final Map<String, PipelineChain> chains       = new HashMap<>();
    private final RootDefinition             rootDefinition;
    private final PipelineProcessorFactory   processorFactory;
    private final ProxyFactory               proxyFactory = new DefaultProxyFactory();

    public PipelineManager(String definition) {
        this.rootDefinition = DefinitionLoader.createLoader(definition).load(definition);
        this.processorFactory = new PipelineProcessorFactory();
    }

    public RootDefinition getRootDefinition() {
        return rootDefinition;
    }

    public PipelineChain createProcessorChain(String chainName) {
        RootDefinition.Chain chainDefinition = rootDefinition.chains().get(chainName);

        if (chainDefinition == null) {
            throw new PipelineDefinitionException("No chain definition found: " + chainName);
        }

        if (chains.containsKey(chainDefinition.name())) {
            return chains.get(chainDefinition.name());
        }

        Map<String, PipelineProcessor>   processors = new HashMap<>();
        Map<String, ProcessorProperties> properties = new HashMap<>();

        chainDefinition.links().forEach((linkName, linkDefinition) -> {
            RootDefinition.ProcessorProperties propertiesDefinition = linkDefinition.properties();
            PipelineProcessor                  processor            = processorFactory.createProcessor(
                    linkDefinition.processor());
            Map<String, String>                transitions          = propertiesDefinition == null ? new HashMap<>()
                    : propertiesDefinition.transitions();
            Map<String, String>                configuration        = propertiesDefinition == null ? new HashMap<>()
                    : propertiesDefinition.configuration();
            Optional<RootDefinition.Fallback> fallback = propertiesDefinition == null ? Optional.empty() : Optional.ofNullable(
                    propertiesDefinition.fallback());

            processors.put(linkName, proxyFactory.createProxy(processor));
            properties.put(linkName, new ProcessorProperties(
                    transitions, configuration, fallback.map(RootDefinition.Fallback::link).orElse(null)));
        });

        PipelineChain chain = new PipelineProcessorChain(chainDefinition.initial(), processors, properties);
        PipelineChain proxy = proxyFactory.createProxy(chain);

        chains.put(chainDefinition.name(), proxy);

        return proxy;
    }

    public void runPipeline(String chainName, PipelineContext context) throws Exception {
        context.getResultContext().clear();
        createProcessorChain(chainName).proceed(context);
    }

}
