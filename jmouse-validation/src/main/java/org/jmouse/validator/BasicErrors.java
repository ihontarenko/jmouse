package org.jmouse.validator;

abstract public class BasicErrors implements Errors {

    private final Object target;

    public BasicErrors(Object target) {
        this.target = target;
    }

    @Override
    public void reject(String errorCode, String defaultMessage, String[] arguments) {

    }

    @Override
    public void rejectValue(String field, String errorCode, String defaultMessage, String[] arguments) {

    }

}
