package org.jmouse.validator.constraint.api;

public interface ConstraintExecutor<T extends Constraint> {

    /**
     * @param value current field value (may be null)
     * @param constraint typed constraint configuration
     * @return true if valid
     */
    boolean test(Object value, T constraint);
}