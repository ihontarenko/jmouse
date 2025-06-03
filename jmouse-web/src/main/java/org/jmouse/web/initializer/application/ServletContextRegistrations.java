package org.jmouse.web.initializer.application;

import org.jmouse.beans.BeanContext;
import org.jmouse.util.Sorter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.servlet.RequestContextListener;
import org.jmouse.web.servlet.WebBeanContextListener;
import org.jmouse.web.servlet.registration.AbstractRegistrationBean;
import org.jmouse.web.servlet.registration.FilterRegistrationBean;
import org.jmouse.web.servlet.registration.ServletListenerRegistrationBean;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ServletContextRegistrations {

    public static final String WEB_BEAN_CONTEXT_LISTENER_NAME = "webBeanContextListener";
    public static final String REQUEST_CONTEXT_LISTENER_NAME  = "requestContextListener";

    public Collection<WebApplicationInitializer> getRegistrationBeanInitializers(BeanContext context) {
        List<WebApplicationInitializer> initializers = new ArrayList<>();

        for (AbstractRegistrationBean bean : context.getBeans(AbstractRegistrationBean.class)) {
            if (bean instanceof ServletRegistrationBean<?> servletRegistrationBean) {
                initializers.add(servletRegistrationBean);
            } else if (bean instanceof FilterRegistrationBean<?> filterRegistrationBean) {
                initializers.add(filterRegistrationBean);
            } else if (bean instanceof ServletListenerRegistrationBean<?> listenerRegistrationBean) {
                initializers.add(listenerRegistrationBean);
            }
        }

        initializePredefined(initializers, context);

        Sorter.sort(initializers);

        return initializers;
    }

    private void initializePredefined(List<WebApplicationInitializer> initializers, BeanContext context) {
        ServletListenerRegistrationBean<?> webBeanContextListener = new ServletListenerRegistrationBean<>(
                WEB_BEAN_CONTEXT_LISTENER_NAME, new WebBeanContextListener((WebBeanContext) context));
        ServletListenerRegistrationBean<?> requestContextListener = new ServletListenerRegistrationBean<>(
                REQUEST_CONTEXT_LISTENER_NAME, new RequestContextListener());

        webBeanContextListener.setEnabled(true);
        requestContextListener.setEnabled(true);

        initializers.add(requestContextListener);
        initializers.add(webBeanContextListener);
    }

}
