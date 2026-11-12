package org.jmouse.web.http.multipart;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.http.HttpStatus;
import org.jmouse.web.mvc.HttpRequestException;

/** Detects and adapts the supplied servlet request, without ambient thread-local state. */
public interface MultipartResolver {
    HttpServletRequest wrapRequest(HttpServletRequest request);

    default boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) return false;
        try {
            return MediaType.forString(contentType).compatible(MediaType.MULTIPART_FORM_DATA);
        } catch (IllegalArgumentException failure) {
            throw new HttpRequestException(HttpStatus.BAD_REQUEST, "Invalid Content-Type header", failure);
        }
    }
}
