package org.jmouse.web.binding;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.mapping.MappingDestination;

public record BindingValue(
        Object current,
        MappingDestination destination,
        PropertyPath path
) {}