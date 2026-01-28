package org.jmouse.core.mapping.plugin;

import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.mapping.MappingContext;

public record MappingFailure(Throwable error, PropertyPath path, MappingContext context) {}

