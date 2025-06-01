package org.jmouse.web.initializer.application;

import org.jmouse.beans.BeanContext;
import org.jmouse.util.Sorter;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.servlet.registration.AbstractRegistrationBean;
import org.jmouse.web.servlet.registration.FilterRegistrationBean;
import org.jmouse.web.servlet.registration.ServletListenerRegistrationBean;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ServletContextRegistrations {

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

        Sorter.sort(initializers);

        return initializers;
    }

}
