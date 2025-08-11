package org.jmouse.web.request.multipart;

import jakarta.servlet.http.HttpServletRequest;

public class SimpleMultipartResolver implements MultipartResolver {

    @Override
    public HttpServletRequest resolveMultipart(HttpServletRequest request) {
        return new MultipartWebHttpRequest(request);
    }

}
