package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jmouse.web.request.http.HttpMethod;

public class WebHttpRequest extends HttpServletRequestWrapper implements WebRequest {

    public WebHttpRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public HttpServletRequest getRequest() {
        return this;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return RequestAttributesHolder.getRequestRoute().method();
    }

}
