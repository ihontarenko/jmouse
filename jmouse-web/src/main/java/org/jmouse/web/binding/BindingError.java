package org.jmouse.web.binding;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.mapping.errors.MappingException;

public record BindingError(
        MappingException error,
        PropertyPath path
) {}
