package org.jmouse.web.http.request.multipart;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.RequestAttributesHolder;

public interface MultipartResolver {

    HttpServletRequest resolveMultipart(HttpServletRequest request);

    default boolean isMultipart(HttpServletRequest request) {
        Headers   headers     = RequestAttributesHolder.getRequestHeaders().headers();
        MediaType contentType = headers.getContentType();

        if (contentType != null) {
            return contentType.compatible(MediaType.MULTIPART_FORM_DATA);
        }

        return false;
    }

}
