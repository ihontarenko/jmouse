package org.jmouse.validator.manual;

public interface ErrorCode {

    String name();

    enum Default implements ErrorCode {

        NULL_OBJECT, STRING_BLANK

    }

}
