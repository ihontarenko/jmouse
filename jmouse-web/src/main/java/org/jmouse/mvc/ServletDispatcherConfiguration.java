package org.jmouse.mvc;

import org.jmouse.beans.annotation.*;
import org.jmouse.context.*;
import org.jmouse.mvc.filter.RequestWrapperFilterRegistration;
import org.jmouse.mvc.filter.SessionServletFilterRegistration;
import org.jmouse.util.SingletonSupplier;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.RequestContextListener;
import org.jmouse.web.servlet.SessionConfigurationInitializer;
import org.jmouse.web.servlet.SessionProperties;
import org.jmouse.web.servlet.WebBeanContextListener;
import org.jmouse.web.servlet.registration.ServletListenerRegistrationBean;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

@BeanFactories
@BeanConditionIfProperty(name = "jmouse.web.enable", value = "true")
public class ServletDispatcherConfiguration {

    public static final String REQUEST_CONTEXT_NAME = "requestContextListener";
    public static final String WEB_SERVLET_CONTEXT_LISTENER_NAME = "webServletContextListener";

    @Bean(proxied = true)
    public ServletRegistrationBean<?> defaultDispatcher(
            ServletDispatcherProperties properties, WebBeanContext rootContext) {
        ServletRegistrationBean<?> registration = new FrameworkDispatcherRegistration(rootContext);

        registration.setEnabled(properties.isEnabled());
        registration.setLoadOnStartup(properties.getLoadOnStartup());
        registration.addMappings(properties.getMappings());

        return registration;
    }

    @Bean
    public SessionConfigurationInitializer sessionInitializer(WebBeanContext rootContext) {
        return new SessionConfigurationInitializer(
                SingletonSupplier.of(() -> rootContext.getBean(SessionProperties.class)));
    }

    @Bean
    public ServletListenerRegistrationBean<RequestContextListener> requestContextListener() {
        ServletListenerRegistrationBean<RequestContextListener> registrationBean = new ServletListenerRegistrationBean<>(
                REQUEST_CONTEXT_NAME, new RequestContextListener());

        registrationBean.setEnabled(true);

        return registrationBean;
    }

    @Bean
    public ServletListenerRegistrationBean<WebBeanContextListener> webBeanContextListener(WebBeanContext rootContext) {
        ServletListenerRegistrationBean<WebBeanContextListener> registrationBean = new ServletListenerRegistrationBean<>(
                WEB_SERVLET_CONTEXT_LISTENER_NAME, new WebBeanContextListener(rootContext));

        registrationBean.setEnabled(true);

        return registrationBean;
    }

    @Bean
    public SessionServletFilterRegistration sessionServletFilterRegistration() {
        return new SessionServletFilterRegistration();
    }

    @Bean
    public RequestWrapperFilterRegistration requestWrapperFilterRegistration() {
        return new RequestWrapperFilterRegistration();
    }

}
