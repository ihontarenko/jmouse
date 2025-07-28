package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.BeanInitializer;
import org.jmouse.mvc.AbstractHandlerMapping;
import org.jmouse.mvc.HandlerInterceptor;
import org.jmouse.mvc.HandlerInterceptorRegistry;
import org.jmouse.mvc.handler.Controller;
import org.jmouse.util.PlaceholderReplacer;
import org.jmouse.util.PlaceholderResolver;
import org.jmouse.util.StandardPlaceholderReplacer;
import org.jmouse.web.WebApplicationFactory;
import org.jmouse.web.context.WebBeanContext;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerPathMapping extends AbstractHandlerMapping {

    private final Map<String, Controller>    controllers;
    private       HandlerInterceptorRegistry registry;
    private       PlaceholderReplacer        placeholderReplacer;

    public ControllerPathMapping() {
        this.controllers = new ConcurrentHashMap<>();
    }

    @BeanInitializer
    public void initialize(BeanContext context) {
        placeholderReplacer = context.getBean(WebApplicationFactory.ROUTE_REPLACER_BEAN_NAME);

        List<ControllerRegistration> registrations = WebBeanContext.getLocalBeans(
                ControllerRegistration.class, (WebBeanContext) context);

        for (ControllerRegistration registration : registrations) {
            addController(registration.route(), registration.controller());
        }

        registry = context.getBean(HandlerInterceptorRegistry.class);
    }

    public void addController(String route, Controller controller) {
        // todo: route should be parsed if any
        // todo: handler should be present like container with path variables etc.

        String parsed = placeholderReplacer.replace(route, (p, d) -> p.toUpperCase(Locale.ROOT) + "( "+ d +" )");

        this.controllers.put(parsed, controller);
    }

    @Override
    protected Object doGetHandler(HttpServletRequest request) {
        String              mappingPath = getMappingPath(request);

        return controllers.get(mappingPath);
    }

    @Override
    protected List<HandlerInterceptor> getHandlerInterceptors() {
        return registry.getInterceptors();
    }


}
