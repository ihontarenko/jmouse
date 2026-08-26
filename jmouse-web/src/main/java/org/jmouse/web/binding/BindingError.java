package org.jmouse.web.binding;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.mapper.errors.MappingException;

public record BindingError(
        MappingException error,
        PropertyPath path
) {}
