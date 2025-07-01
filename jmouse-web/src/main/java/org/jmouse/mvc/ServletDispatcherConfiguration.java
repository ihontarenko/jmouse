package org.jmouse.mvc;

import org.jmouse.beans.BeanLookupStrategy;
import org.jmouse.beans.annotation.*;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.context.BeanConditionIfProperty;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;
import org.jmouse.util.SingletonSupplier;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.SessionConfigurationInitializer;
import org.jmouse.web.servlet.SessionProperties;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

import java.util.List;
import java.util.Set;

@BeanFactories
@BeanConditionIfProperty(name = "jmouse.web.enable", value = "true")
public class ServletDispatcherConfiguration {

    @Bean("requestPathMappingRegistrations")
    public List<DirectRequestPathMapping.Registration> requestPathMappingRegistrations(
            @AggregatedBeans Set<DirectRequestPathMapping.Registration> registrations) {
        return List.copyOf(registrations);
    }

    @Bean("defaultDispatcherContext")
    public WebBeanContext webDefaultBeanContext(WebBeanContext rootContext) {
        @SuppressWarnings("unchecked")
        ApplicationFactory<WebBeanContext> factory = rootContext.getBean(ApplicationFactory.class);
        WebBeanContext webBeanContext = factory.createContext(
                "DEFAULT_DISPATCHER_CONTEXT", rootContext);

        webBeanContext.setBeanLookupStrategy(BeanLookupStrategy.INHERIT_DEFINITION);
        webBeanContext.registerBean("dispatcher", "DEFAULT DISPATCHER!!!");

        return webBeanContext;
    }

    @Bean
    public ServletRegistrationBean<?> defaultDispatcher(
            @Qualifier("defaultDispatcherContext") WebBeanContext webBeanContext,
            ServletDispatcherProperties properties) {
        ServletRegistrationBean<?> registration = new FrameworkDispatcherRegistration(webBeanContext);

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
