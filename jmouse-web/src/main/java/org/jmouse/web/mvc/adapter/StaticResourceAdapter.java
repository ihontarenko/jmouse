package org.jmouse.web.mvc.adapter;


import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.HandlerAdapter;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.mvc.MappedHandler;
import org.jmouse.web.mvc.RouteMatch;
import org.jmouse.web.mvc.resource.ResourceRegistration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StaticResourceAdapter implements HandlerAdapter, InitializingBeanSupport<WebBeanContext> {

    private PatternMatcherResourceLoader loader;

    @Override
    public boolean supportsHandler(MappedHandler handler) {
        return handler.handler() instanceof ResourceRegistration;
    }

    @Override
    public MVCResult handle(HttpServletRequest request, HttpServletResponse response, MappedHandler handler) {
        RouteMatch           match        = handler.mappingResult().match();
        ResourceRegistration registration = (ResourceRegistration) handler.handler();

        if (match != null && match.getVariable("filepath", null) instanceof String filepath) {

            for (String location : registration.getLocations()) {
                String resourcePath = "%s%s".formatted(location, filepath);

                try {
                    Resource resource = loader.getResource(resourcePath);

                    System.out.println(resource.getFile());

                } catch (Exception ignore) { }
            }

        }

        return null;
    }

    @Override
    public void afterCompletion(BeanContext context) {
        loader = context.getBean(PatternMatcherResourceLoader.class);
    }
}
