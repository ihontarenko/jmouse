package org.jmouse.common.pipeline.definition.model;

public record LinkDefinition(
        String name,
        ProcessorDefinition processor,
        LinkProperties properties
) { }