package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HandlerExceptionResolver {

    boolean supports(Exception exception);

    Object resolveException(
            HttpServletRequest request, HttpServletResponse response, Exception exception, MappedHandler handler);

}
