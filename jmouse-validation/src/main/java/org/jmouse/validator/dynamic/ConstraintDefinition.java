package org.jmouse.validator.dynamic;

import java.util.List;

public interface ConstraintDefinition {
    String id();                       // canonical: "minMax"
    List<String> aliases();             // "min", "max", "range" etc (optional)
    ConstraintParameters parameters();  // declared parameters schema
    ConstraintExecutor executor();      // actual logic
}
