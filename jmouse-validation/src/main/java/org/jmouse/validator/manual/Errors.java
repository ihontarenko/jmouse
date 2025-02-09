package org.jmouse.validator.manual;

import java.util.HashSet;

final public class Errors extends HashSet<ErrorMessage> {

    public void rejectValue(String field, String message, String errorCode) {
        add(new ErrorMessage(field, message, errorCode));
    }

    public void rejectValue(String field, String message) {
        rejectValue(field, message, null);
    }

}
