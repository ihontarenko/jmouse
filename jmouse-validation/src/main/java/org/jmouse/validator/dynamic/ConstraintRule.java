package org.jmouse.validator.dynamic;

import java.util.Map;

public record ConstraintRule(
        String id,
        Map<String, Object> arguments,
        String message
) {}