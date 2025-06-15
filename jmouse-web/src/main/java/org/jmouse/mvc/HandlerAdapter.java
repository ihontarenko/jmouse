package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HandlerAdapter {

    boolean supports(Object handler);

    HandlerResponse handle(HttpServletRequest request, HttpServletResponse response, Object handler);

}
