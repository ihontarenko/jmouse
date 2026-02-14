package org.jmouse.validator.dynamic;

import java.util.List;

public record ValidationSchema(
        String formName,
        List<FieldRule> rules
) {}