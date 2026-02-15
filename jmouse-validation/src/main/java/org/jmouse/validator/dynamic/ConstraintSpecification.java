package org.jmouse.validator.dynamic;

import java.util.Map;

public record ConstraintSpecification(
        String constraintId, // "minMax" or "min"
        Map<String, Object> arguments,
        String messageOverride
) {}