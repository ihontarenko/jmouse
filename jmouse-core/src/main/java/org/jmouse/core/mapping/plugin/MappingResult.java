package org.jmouse.core.mapping.plugin;

import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

public record MappingResult(Object source, Object target, InferredType targetType, MappingContext context) {}
