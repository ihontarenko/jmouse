package org.jmouse.core.mapping.plugin;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.errors.MappingException;

public record MappingFailure(MappingException error, PropertyPath path, MappingContext context) {}

