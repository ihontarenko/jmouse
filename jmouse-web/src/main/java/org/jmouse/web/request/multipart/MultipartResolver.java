package org.jmouse.web.request.multipart;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.web.request.Headers;
import org.jmouse.web.request.RequestAttributesHolder;

public interface MultipartResolver {

    HttpServletRequest resolveMultipart(HttpServletRequest request);

    default boolean isMultipart(HttpServletRequest request) {
        Headers headers = RequestAttributesHolder.getRequestHeaders().headers();

        if (headers != null) {
            return headers.getContentType().compatible(MediaType.MULTIPART_FORM_DATA);
        }

        return MediaType.forString(request.getContentType()).compatible(MediaType.MULTIPART_FORM_DATA);
    }

}
