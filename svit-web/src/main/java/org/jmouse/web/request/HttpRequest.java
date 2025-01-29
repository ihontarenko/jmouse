package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;

public interface HttpRequest extends RequestAttributes {

    HttpServletRequest getRequest();
    
}
