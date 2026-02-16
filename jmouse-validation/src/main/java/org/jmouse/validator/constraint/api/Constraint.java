package org.jmouse.validator.constraint.api;

public interface Constraint {

    /**
     * Stable identifier used as error code suffix, registry key, and DSL name.
     *
     * Examples: "minMax", "oneOf", "required".
     */
    String code();

    /**
     * Default message used when constraint fails (may be null).
     */
    default String message() {
        return null;
    }

    /**
     * Executor that evaluates this constraint.
     */
    ConstraintExecutor<? extends Constraint> executor();

    default boolean execute(Object value) {
        @SuppressWarnings("unchecked")
        ConstraintExecutor<Constraint> executor = (ConstraintExecutor<Constraint>) executor();
        return executor.test(value, this);
    }
}