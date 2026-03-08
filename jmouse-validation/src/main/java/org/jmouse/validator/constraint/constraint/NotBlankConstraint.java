package org.jmouse.validator.constraint.constraint;

import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

public class NotBlankConstraint implements Constraint {

    private String message;

    @Override
    public String code() {
        return "not_blank";
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
        return (value, constraint) -> Strings.isNotEmpty(String.valueOf(value));
    }

}
