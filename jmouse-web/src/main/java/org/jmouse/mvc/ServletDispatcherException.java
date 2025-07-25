package org.jmouse.mvc;

import java.io.IOException;

public class ServletDispatcherException extends IOException {

    public ServletDispatcherException() {
    }

    public ServletDispatcherException(String message) {
        super(message);
    }

    public ServletDispatcherException(String message, Throwable cause) {
        super(message, cause);
    }

}
