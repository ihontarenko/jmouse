package org.jmouse.common.support.context;

public record ErrorDetails(String code, String message, Throwable cause) {

    public ErrorDetails(String code, String message) {
        this(code, message, null);
    }

}
