package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.request.http.HttpMethod;

public interface WebRequest extends RequestAttributes {

    HttpServletRequest getRequest();

    HttpMethod getHttpMethod();

}
