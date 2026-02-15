package org.jmouse.validator.constraint.model;

import org.jmouse.core.access.PropertyPath;

import java.util.List;

public interface FieldRules {

    /**
     * original path string, for Errors.rejectValue(...)
     */
    String path();

    /**
     * normalized/parsed path (optional for raw impl)
     */
    default PropertyPath propertyPath() {
        return PropertyPath.forPath(path());
    }

    List<ConstraintRule> rules();
}
