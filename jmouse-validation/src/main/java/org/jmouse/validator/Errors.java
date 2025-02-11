package org.jmouse.validator;

public interface Errors {

    void reject(String errorCode, String defaultMessage, String[] arguments);

    void rejectValue(String field, String errorCode, String defaultMessage, String[] arguments);

}
