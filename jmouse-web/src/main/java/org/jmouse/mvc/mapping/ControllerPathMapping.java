package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.BeanInitializer;
import org.jmouse.mvc.AbstractHandlerMapping;
import org.jmouse.mvc.HandlerInterceptor;
import org.jmouse.mvc.HandlerInterceptorRegistry;
import org.jmouse.mvc.handler.Controller;
import org.jmouse.web.context.WebBeanContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerPathMapping extends AbstractHandlerMapping {

    private final Map<String, Controller> controllers;
    private HandlerInterceptorRegistry registry;

    @BeanInitializer
    public void initialize(BeanContext context) {
        List<ControllerRegistration> registrations = WebBeanContext.getLocalBeans(
                ControllerRegistration.class, (WebBeanContext) context);

        for (ControllerRegistration registration : registrations) {
            addController(registration.route(), registration.controller());
        }

        registry = context.getBean(HandlerInterceptorRegistry.class);
    }

    public ControllerPathMapping() {
        this.controllers = new ConcurrentHashMap<>();
    }

    public void addController(String route, Controller controller) {
        this.controllers.put(route, controller);
    }

    @Override
    protected Object doGetHandler(HttpServletRequest request) {
        return controllers.get(getMappingPath(request));
    }

    @Override
    protected List<HandlerInterceptor> getHandlerInterceptors() {
        return registry.getInterceptors();
    }


}
