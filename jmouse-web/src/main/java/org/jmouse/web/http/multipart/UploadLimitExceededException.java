package org.jmouse.web.http.multipart;

public class UploadLimitExceededException extends MultipartRequestException {
    public UploadLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
