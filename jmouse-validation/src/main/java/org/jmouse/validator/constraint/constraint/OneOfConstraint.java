package org.jmouse.validator.constraint.constraint;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.util.ArrayList;
import java.util.List;

public final class OneOfConstraint implements Constraint {

    /**
     * Allowed values. Stored as strings for stable comparison across scalar types.
     */
    private List<String> allowed;

    private String message;

    @Override
    public String code() {
        return "one_of";
    }

    @Override
    public String message() {
        return getMessage();
    }

    public List<String> getAllowed() {
        return allowed;
    }

    public void setAllowed(List<String> allowed) {
        this.allowed = (allowed == null) ? new ArrayList<>() : new ArrayList<>(allowed);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public ConstraintExecutor<OneOfConstraint> executor() {
        return Executor.INSTANCE;
    }

    static final class Executor implements ConstraintExecutor<OneOfConstraint> {

        static final Executor INSTANCE = new Executor();

        private Executor() {}

        @Override
        public boolean test(Object value, OneOfConstraint c) {
            if (value == null) {
                return true;
            }
            if (c.allowed == null || c.allowed.isEmpty()) {
                return true; // MVP: no allowed list => pass (or choose strict later)
            }

            String normalized = String.valueOf(value);
            return c.allowed.contains(normalized);
        }
    }
}
