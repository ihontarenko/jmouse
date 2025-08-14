package org.jmouse.validator.old;

import org.jmouse.util.Strings;

import static org.jmouse.util.Strings.underscored;

abstract public class AbstractValidator implements Validator {

    protected String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "%s [message='%s']".formatted(Strings.underscored(this.getClass().getSimpleName(), true), message);
    }

}
