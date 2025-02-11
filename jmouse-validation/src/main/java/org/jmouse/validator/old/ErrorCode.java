package org.jmouse.validator.old;

public interface ErrorCode {

    String name();

    enum Default implements ErrorCode {

        NULL_OBJECT, STRING_BLANK

    }

}
