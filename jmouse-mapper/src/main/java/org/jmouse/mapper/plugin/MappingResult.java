package org.jmouse.mapper.plugin;

import org.jmouse.mapper.MappingContext;
import org.jmouse.core.reflection.InferredType;

public record MappingResult(Object source, Object target, InferredType targetType, MappingContext context) {}
