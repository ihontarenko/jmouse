package org.jmouse.web.http.request.multipart;

import jakarta.servlet.http.HttpServletRequest;

public class SimpleMultipartResolver implements MultipartResolver {

    @Override
    public HttpServletRequest wrapRequest(HttpServletRequest request) {
        return new MultipartWebHttpRequest(request);
    }

}
