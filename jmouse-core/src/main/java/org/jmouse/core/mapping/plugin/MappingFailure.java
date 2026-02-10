package org.jmouse.core.mapping.plugin;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.mapping.MappingContext;

public record MappingFailure(Throwable error, PropertyPath path, MappingContext context) {}

