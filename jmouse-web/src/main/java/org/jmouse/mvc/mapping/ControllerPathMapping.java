package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.BeanInitializer;
import org.jmouse.mvc.*;
import org.jmouse.mvc.handler.Controller;
import org.jmouse.web.context.WebBeanContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerPathMapping extends AbstractHandlerMapping {

    private final Map<PathPattern, Controller> controllers;
    private       HandlerInterceptorRegistry   registry;

    public ControllerPathMapping() {
        this.controllers = new ConcurrentHashMap<>();
    }

    @BeanInitializer
    public void initialize(BeanContext context) {
        List<ControllerRegistration> registrations = WebBeanContext.getLocalBeans(
                ControllerRegistration.class, (WebBeanContext) context);

        for (ControllerRegistration registration : registrations) {
            addController(registration.route(), registration.controller());
        }

        registry = context.getBean(HandlerInterceptorRegistry.class);
    }

    public void addController(String route, Controller controller) {
        this.controllers.put(new PathPattern(route), controller);
    }

    @Override
    protected Object doGetHandler(HttpServletRequest request) {
        String mappingPath = getMappingPath(request);

        Object handler = null;

        for (Map.Entry<PathPattern, Controller> entry : controllers.entrySet()) {
            PathPattern pathPattern = entry.getKey();
            if (pathPattern.matches(mappingPath)) {
                RoutePath routePath = pathPattern.parse(mappingPath);
                handler = new MappedHandler(entry.getValue(), routePath);
            }
        }

        return handler;
    }

    @Override
    protected List<HandlerInterceptor> getHandlerInterceptors() {
        return registry.getInterceptors();
    }


}
