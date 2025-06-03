package org.jmouse.web._app;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.web.servlet.DelegatingBeanFilterRegistration;
import org.jmouse.web.servlet.registration.FilterRegistrationBean;

@Configuration
public class Configurations {

    @Provide
    public FilterRegistrationBean<?> appFilterRegistration(BeanContext context) {
        FilterRegistrationBean<?> registration = new DelegatingBeanFilterRegistration("appFilter");

        registration.setEnabled(true);
        registration.addUrlPatterns("/*");

        return registration;
    }

}
