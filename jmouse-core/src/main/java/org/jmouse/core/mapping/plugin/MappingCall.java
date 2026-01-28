package org.jmouse.core.mapping.plugin;

import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

public record MappingCall(Object source, Class<?> sourceType, InferredType targetType, MappingContext context) {}

