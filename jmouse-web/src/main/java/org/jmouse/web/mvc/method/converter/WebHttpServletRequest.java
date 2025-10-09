package org.jmouse.web.mvc.method.converter;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.Headers;
import org.jmouse.web.http.RequestAttributesHolder;
import org.jmouse.web.http.WebHttpRequest;

public class WebHttpServletRequest extends WebHttpRequest implements HttpInputMessage {

    public WebHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public Headers getHeaders() {
        return RequestAttributesHolder.getRequestHeaders().headers();
    }

}
