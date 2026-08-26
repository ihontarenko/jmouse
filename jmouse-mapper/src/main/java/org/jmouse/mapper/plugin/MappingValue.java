package org.jmouse.mapper.plugin;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.MappingDestination;
import org.jmouse.core.reflection.InferredType;

public record MappingValue(
        Object rootSource,
        Object current,
        InferredType targetType,
        PropertyPath path,           // user.name / details[0]
        MappingContext context,
        MappingDestination destination
) {}