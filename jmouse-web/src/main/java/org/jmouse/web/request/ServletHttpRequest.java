package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;

public class ServletHttpRequest implements ServletRequest {

    private final HttpServletRequest request;

    public ServletHttpRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        request.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        request.removeAttribute(name);
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

}
