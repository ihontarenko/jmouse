package org.jmouse.validator.dynamic;

import java.util.List;

public record FieldRule(
        String path,              // "profile.email", "items[0].qty", etc
        List<ConstraintRule> constraints
) {}