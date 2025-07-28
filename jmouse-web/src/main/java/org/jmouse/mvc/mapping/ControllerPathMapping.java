package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.BeanInitializer;
import org.jmouse.mvc.HandlerInterceptorRegistry;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.mvc.handler.Controller;
import org.jmouse.web.context.WebBeanContext;

import java.util.List;

public class ControllerPathMapping extends AbstractHandlerPathMapping<Controller> {

    @BeanInitializer
    public void initialize(BeanContext context) {
        List<ControllerRegistration> registrations = WebBeanContext.getLocalBeans(
                ControllerRegistration.class, (WebBeanContext) context);

        for (ControllerRegistration registration : registrations) {
            addHandlerMapping(registration.route(), registration.controller());
        }

        setHandlerInterceptorsRegistry(
                context.getBean(HandlerInterceptorRegistry.class)
        );
    }

    @Override
    protected Object doGetHandler(HttpServletRequest request) {
        String        mappingPath   = getMappingPath(request);
        MappedHandler mappedHandler = getMappedHandler(mappingPath);

        if (mappedHandler == null) {

        }

        return mappedHandler;
    }

}
