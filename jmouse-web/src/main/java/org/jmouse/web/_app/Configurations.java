package org.jmouse.web._app;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.web.servlet.DelegatingBeanFilterRegistration;
import org.jmouse.web.servlet.registration.FilterRegistrationBean;

import java.util.UUID;

@Configuration
public class Configurations {

    @Provide(value = "rqUUID", scope = BeanScope.REQUEST)
    public String getRequestScopedValue() {
        System.out.println("getRequestScopedValue called!");
        return UUID.randomUUID().toString();
    }

    @Provide
    public FilterRegistrationBean<?> appFilterRegistration(BeanContext context) {
        FilterRegistrationBean<?> registration = new DelegatingBeanFilterRegistration("appFilter");

        registration.setEnabled(true);
        registration.addUrlPatterns("/*");

        return registration;
    }

    @Provide
    public FilterRegistrationBean<?> logFilterRegistration(BeanContext context) {
        FilterRegistrationBean<?> registration = new DelegatingBeanFilterRegistration("logFilter");

        registration.setEnabled(true);
        registration.addUrlPatterns("/*");

        return registration;
    }

}
