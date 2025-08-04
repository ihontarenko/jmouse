package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface RequestContext {

    HttpServletRequest request();

    HttpServletResponse response();

    record Default(HttpServletRequest request, HttpServletResponse response) implements RequestContext {}

    static RequestContext of(HttpServletRequest request, HttpServletResponse response) {
        return new Default(request, response);
    }

}
