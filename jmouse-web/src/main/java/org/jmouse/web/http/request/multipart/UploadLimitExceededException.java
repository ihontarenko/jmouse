package org.jmouse.web.http.request.multipart;

public class UploadLimitExceededException extends MultipartRequestException {
    public UploadLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
