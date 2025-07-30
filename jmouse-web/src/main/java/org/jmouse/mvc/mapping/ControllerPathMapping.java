package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.handler.Controller;
import org.jmouse.web.context.WebBeanContext;

public class ControllerPathMapping extends AbstractHandlerPathMapping<Controller> {

    @Override
    protected Object doGetHandler(HttpServletRequest request) {
        return getMappedHandler(request);
    }

    @Override
    protected void doInitialize(WebBeanContext context) {
        for (ControllerRegistration registration : WebBeanContext.getLocalBeans(
                ControllerRegistration.class, context)) {
            addHandlerMapping(registration.route(), registration.controller());
        }
    }
}
