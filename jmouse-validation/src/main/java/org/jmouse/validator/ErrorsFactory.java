package org.jmouse.validator;

public interface ErrorsFactory {
    Errors create(Object target, String objectName);
}
