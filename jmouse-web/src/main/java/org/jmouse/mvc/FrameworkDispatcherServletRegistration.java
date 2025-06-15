package org.jmouse.mvc;

import org.jmouse.beans.annotation.Ignore;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

@Ignore
public class FrameworkDispatcherServletRegistration extends ServletRegistrationBean<FrameworkDispatcherServlet> {

    public FrameworkDispatcherServletRegistration(WebBeanContext context) {
        super("defaultDispatcher", new FrameworkDispatcherServlet(context));
    }

}
