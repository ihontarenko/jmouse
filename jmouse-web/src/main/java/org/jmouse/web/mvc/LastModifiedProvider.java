package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.Headers;
import org.jmouse.web.mvc.method.HandlerMethod;

public interface LastModifiedProvider {

    long provide(HandlerMethod method, MappingResult result, HttpServletRequest request, Headers headers);

}