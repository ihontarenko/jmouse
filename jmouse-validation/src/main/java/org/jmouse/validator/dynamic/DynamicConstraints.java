package org.jmouse.validator.dynamic;

public interface DynamicConstraints {
    ConstraintExecutor get(String id);
}