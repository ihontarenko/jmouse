package org.jmouse.mapper.plugin;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.errors.MappingException;

public record MappingFailure(MappingException error, PropertyPath path, MappingContext context) {}

