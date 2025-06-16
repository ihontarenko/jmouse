package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.HandlerMapping;
import org.jmouse.mvc.handler.Controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DirectMapping implements HandlerMapping {

    private final Map<String, Controller> controllers;

    public DirectMapping() {
        this.controllers = new ConcurrentHashMap<>();
    }

    public void addController(String route, Controller controller) {
        this.controllers.put(route, controller);
    }

    @Override
    public Object getHandler(HttpServletRequest request) {
        return controllers.get(request.getRequestURI());
    }
}
