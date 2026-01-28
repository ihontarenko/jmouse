package org.jmouse.core.mapping.plugin;

import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

public record MappingValue(
        Object rootSource,
        Object current,
        InferredType targetType,
        PropertyPath path,           // user.name / details[0]
        MappingContext context
) {}