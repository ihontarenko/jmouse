package org.jmouse.web.mvc;

import java.util.Objects;
import org.jmouse.http.HttpStatus;

/** Explicit client request failure; internal causes are retained without becoming response text. */
public final class HttpRequestException extends RuntimeException {

    private final HttpStatus status;

    public HttpRequestException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public HttpRequestException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = Objects.requireNonNull(status, "status");
        if (status.getCode() < 400 || status.getCode() >= 500) {
            throw new IllegalArgumentException("Request exception requires a client error status");
        }
    }

    public HttpStatus status() {
        return status;
    }
}
