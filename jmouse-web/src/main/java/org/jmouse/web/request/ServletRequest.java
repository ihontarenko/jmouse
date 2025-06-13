package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;

public interface ServletRequest extends RequestAttributes {

    HttpServletRequest getRequest();
    
}
