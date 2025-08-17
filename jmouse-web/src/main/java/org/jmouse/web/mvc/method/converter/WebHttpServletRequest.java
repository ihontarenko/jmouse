package org.jmouse.web.mvc.method.converter;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.WebHttpRequest;

public class WebHttpServletRequest extends WebHttpRequest implements HttpInputMessage {

    public WebHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public Headers getHeaders() {
        return RequestAttributesHolder.getRequestHeaders().headers();
    }

}
