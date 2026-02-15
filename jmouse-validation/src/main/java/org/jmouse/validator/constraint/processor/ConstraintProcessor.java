package org.jmouse.validator.constraint.processor;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

public final class ConstraintProcessor {

    /**
     * Evaluate a single constraint against a value.
     *
     * <p>Engine is intentionally minimal:
     * it does not know about errors, paths, schemas, EL, or mapping.</p>
     *
     * @param value field value (may be {@code null})
     * @param constraint typed constraint configuration
     * @return {@code true} when constraint passes
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean test(Object value, Constraint constraint) {
        if (constraint == null) {
            return true;
        }

        ConstraintExecutor executor = constraint.executor();

        if (executor == null) {
            return true;
        }

        return executor.test(value, constraint);
    }
}
