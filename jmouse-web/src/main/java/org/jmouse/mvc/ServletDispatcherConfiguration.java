package org.jmouse.mvc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.beans.annotation.*;
import org.jmouse.context.*;
import org.jmouse.util.SingletonSupplier;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.SessionConfigurationInitializer;
import org.jmouse.web.servlet.SessionProperties;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

@BeanFactories
@BeanConditionIfProperty(name = "jmouse.web.enable", value = "true")
@BeanConditionForRootContext
@BeanConditionExpression(value = "jmouse.web.enable eq 'true'")
public class ServletDispatcherConfiguration implements BeanContextAware {

    public static final String CONTEXT_PREFIX = "Context";

    private BeanContext context;

    @Bean
    public ServletContextManager servletContextManager(WebBeanContext rootContext) {
        return new ServletContextManager(rootContext);
    }

    @Bean(proxied = true)
    public ServletRegistrationBean<?> defaultDispatcher(
            ServletContextManager servletContextManager, ServletDispatcherProperties properties) {
        WebBeanContext dispatcherContext = servletContextManager.createDispatcherContext(
                properties.getName() + CONTEXT_PREFIX, getBeanContext().getBaseClasses());
        ServletRegistrationBean<?> registration = new FrameworkDispatcherRegistration(dispatcherContext);

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

    /**
     * Sets the {@link BeanContext} for this component.
     *
     * @param context the {@link BeanContext} to set.
     */
    @Override
    public void setBeanContext(BeanContext context) {
        this.context = context;
    }

    /**
     * Retrieves the {@link BeanContext} associated with this component.
     *
     * @return the current {@link BeanContext}.
     */
    @Override
    public BeanContext getBeanContext() {
        return context;
    }
}
