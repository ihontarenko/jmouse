package org.jmouse.web.http.multipart;

public class MultipartRequestException extends RuntimeException {

    public MultipartRequestException(String message) {
        super(message);
    }

    public MultipartRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultipartRequestException(Throwable cause) {
        super(cause);
    }

}
