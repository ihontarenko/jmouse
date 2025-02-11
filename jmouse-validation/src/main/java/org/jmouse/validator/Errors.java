package org.jmouse.validator;

public interface Errors {

    void reject(String errorCode, String[] arguments, String defaultMessage);

    void rejectValue(String field, String errorCode, String[] arguments, String defaultMessage);

}
