package org.jmouse.validator.old;

public interface ErrorContext {

    String name();

    enum Default implements ErrorContext {

        EMPTY, DEFAULT

    }

}
