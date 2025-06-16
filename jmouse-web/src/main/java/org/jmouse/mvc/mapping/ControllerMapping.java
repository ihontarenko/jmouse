package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.HandlerMapping;

public class ControllerMapping implements HandlerMapping {

    @Override
    public Object getHandler(HttpServletRequest request) {

        System.out.println(request.getRequestURI());

        return null;
    }

}
