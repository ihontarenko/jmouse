package org.jmouse.validator.manual;

public interface ErrorContext {

    String name();

    enum Default implements ErrorContext {

        EMPTY, DEFAULT

    }

}
