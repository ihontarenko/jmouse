package org.jmouse.validator.constraint.constraint;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

public class RequiredConstraint implements Constraint {

    private String message;

    @Override
    public String code() {
        return "required";
    }

    @Override
    public String message() {
        return message;
    }

    /**
     * @return custom validation message (may be {@code null})
     */
    public String getMessage() {
        return message();
    }

    /**
     * Sets custom validation message.
     *
     * @param message message override
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public ConstraintExecutor<? extends Constraint> executor() {
        return (value, constraint) -> value != null;
    }

}
