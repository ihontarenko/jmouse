package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.converter.HttpInputMessage;
import org.jmouse.web.request.Headers;
import org.jmouse.web.request.RequestAttributesHolder;
import org.jmouse.web.request.WebHttpRequest;

public class WebHttpServletRequest extends WebHttpRequest implements HttpInputMessage {

    public WebHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public Headers getHeaders() {
        return RequestAttributesHolder.getRequestHeaders().headers();
    }

}
