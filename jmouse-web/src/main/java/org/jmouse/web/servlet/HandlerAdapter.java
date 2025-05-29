package org.jmouse.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HandlerAdapter {

    boolean supports(Object handler);

    HandlerResult handle(HttpServletRequest rq, HttpServletResponse rs, Object handler) throws Exception;

}
