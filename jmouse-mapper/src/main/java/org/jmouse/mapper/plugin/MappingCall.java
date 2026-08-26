package org.jmouse.mapper.plugin;

import org.jmouse.mapper.MappingContext;
import org.jmouse.core.reflection.InferredType;

public record MappingCall(Object source, Class<?> sourceType, InferredType targetType, MappingContext context) {}

