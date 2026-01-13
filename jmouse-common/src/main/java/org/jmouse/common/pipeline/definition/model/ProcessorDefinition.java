package org.jmouse.common.pipeline.definition.model;

import java.util.List;

public record ProcessorDefinition(
        String className,
        List<ParameterDefinition> parameters
) { }