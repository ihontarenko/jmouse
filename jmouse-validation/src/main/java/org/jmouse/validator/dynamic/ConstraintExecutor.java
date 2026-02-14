package org.jmouse.validator.dynamic;

import java.util.Map;

@FunctionalInterface
public interface ConstraintExecutor {
    boolean test(Object value, Map<String, Object> arguments);
}