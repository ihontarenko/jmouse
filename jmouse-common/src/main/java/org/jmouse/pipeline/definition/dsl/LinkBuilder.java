package org.jmouse.pipeline.definition.dsl;

import org.jmouse.pipeline.definition.model.LinkDefinition;
import org.jmouse.pipeline.definition.model.LinkProperties;
import org.jmouse.pipeline.definition.model.ParameterDefinition;
import org.jmouse.pipeline.definition.model.ProcessorDefinition;
import org.jmouse.pipeline.PipelineProcessor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LinkBuilder {

    private final String name;

    private String processorClassName;

    private final Map<String, String> transitions = new LinkedHashMap<>();
    private final Map<String, String> configuration = new LinkedHashMap<>();
    private String fallback;

    private final List<ParameterDefinition> parameters = new ArrayList<>();

    LinkBuilder(String name) {
        this.name = name;
    }

    public LinkBuilder processor(Class<? extends PipelineProcessor> type) {
        this.processorClassName = type.getName();
        return this;
    }

    public LinkBuilder processor(String className) {
        this.processorClassName = className;
        return this;
    }

    public LinkBuilder onReturn(String returnCode, String linkName) {
        transitions.put(returnCode, linkName);
        return this;
    }

    public LinkBuilder fallback(String linkName) {
        this.fallback = linkName;
        return this;
    }

    public LinkBuilder config(String key, String value) {
        configuration.put(key, value);
        return this;
    }

    public LinkBuilder param(String name, String value, String resolver, String converter) {
        parameters.add(new ParameterDefinition(name, value, resolver, converter));
        return this;
    }

    LinkDefinition build() {
        ProcessorDefinition processor = new ProcessorDefinition(processorClassName, List.copyOf(parameters));
        LinkProperties props = new LinkProperties(transitions, configuration, fallback);
        return new LinkDefinition(name, processor, props);
    }
}
