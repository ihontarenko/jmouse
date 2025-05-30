package org.jmouse.web.servlet;

import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

public class FrameworkDispatcherServletRegistration extends ServletRegistrationBean<FrameworkDispatcherServlet> {

    @BeanConstructor
    public FrameworkDispatcherServletRegistration(WebBeanContext context) {
        this("defaultDispatcher", new FrameworkDispatcherServlet(context));
    }

    public FrameworkDispatcherServletRegistration(String name, FrameworkDispatcherServlet servlet) {
        super(name, servlet);
    }

}
