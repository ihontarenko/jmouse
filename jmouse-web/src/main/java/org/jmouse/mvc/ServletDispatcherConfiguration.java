package org.jmouse.mvc;

import org.jmouse.beans.annotation.*;
import org.jmouse.context.*;
import org.jmouse.util.SingletonSupplier;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.SessionConfigurationInitializer;
import org.jmouse.web.servlet.SessionProperties;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

@BeanFactories
@BeanConditionIfProperty(name = "jmouse.web.enable", value = "true")
public class ServletDispatcherConfiguration {

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

}
