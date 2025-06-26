package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanInitializer;
import org.jmouse.mvc.AbstractHandlerMapping;
import org.jmouse.mvc.HandlerInterceptor;
import org.jmouse.mvc.handler.Controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Bean
public class DirectRequestPathMapping extends AbstractHandlerMapping {

    private final Map<String, Controller> controllers;

    @BeanInitializer
    public void init(BeanContext context) {
        for (Registration registration : context.getBeans(Registration.class)) {
            addController(registration.getRoute(), registration.getController());
        }
    }

    public DirectRequestPathMapping() {
        this.controllers = new ConcurrentHashMap<>();
    }

    public void addController(String route, Controller controller) {
        this.controllers.put(route, controller);
    }

    @Override
    protected Object doGetHandler(HttpServletRequest request) {
        return controllers.get(request.getRequestURI());
    }

    @Override
    protected List<HandlerInterceptor> getHandlerInterceptors() {
        return List.of();
    }

    public static class Registration {

        private String route;
        private Controller controller;

        public Registration(String route, Controller controller) {
            this.route = route;
            this.controller = controller;
        }

        public String getRoute() {
            return route;
        }

        public Controller getController() {
            return controller;
        }

        public void setController(Controller controller) {
            this.controller = controller;
        }

        public void setRoute(String route) {
            this.route = route;
        }

        @Override
        public String toString() {
            return route;
        }

    }

}
