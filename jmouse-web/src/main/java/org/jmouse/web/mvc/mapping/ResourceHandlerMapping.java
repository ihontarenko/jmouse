package org.jmouse.web.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.matcher.ant.AntMatcher;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.Handler;
import org.jmouse.web.mvc.HandlerMapping;
import org.jmouse.web.mvc.resource.MappingRegistry;
import org.jmouse.web.mvc.resource.ResourceHandlerRegistration;

public class ResourceHandlerMapping implements HandlerMapping, InitializingBeanSupport<WebBeanContext> {

    private final MappingRegistry mappingRegistry;

    public ResourceHandlerMapping() {
        this(null);
    }

    public ResourceHandlerMapping(MappingRegistry mappingRegistry) {
        this.mappingRegistry = mappingRegistry;
    }

    public ResourceHandlerRegistration addResourceResolver(String... patterns) {
        ResourceHandlerRegistration registration = new ResourceHandlerRegistration();

        for (String pattern : patterns) {
            AntMatcher matcher = new AntMatcher(pattern);
            registration.setAntMatcher(matcher);
//            mappingRegistry.put(matcher, registration);
        }

        return registration;
    }

    @Override
    public Handler getHandler(HttpServletRequest request) {
        return null;
    }

    @Override
    public boolean supportsMappedHandler(Object mapped) {
        return false;
    }

}
