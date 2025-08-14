package org.jmouse.web.http.request.multipart;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.RequestAttributesHolder;

public interface MultipartResolver {

    HttpServletRequest wrapRequest(HttpServletRequest request);

    default boolean isMultipart(HttpServletRequest request) {
        Headers   headers = RequestAttributesHolder.getRequestHeaders().headers();
        MediaType contentType;

        if (headers != null && (contentType = headers.getContentType()) != null) {
            return contentType.compatible(MediaType.MULTIPART_FORM_DATA);
        } else {
            String rawContentType = request.getContentType();
            if (rawContentType != null) {
                return MediaType.forString(rawContentType).compatible(MediaType.MULTIPART_FORM_DATA);
            }
        }

        return false;
    }

}
