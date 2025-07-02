package org.jmouse.mvc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.beans.BeanLookupStrategy;
import org.jmouse.beans.annotation.*;
import org.jmouse.context.*;
import org.jmouse.core.bind.BindName;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;
import org.jmouse.util.SingletonSupplier;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.server.WebServers;
import org.jmouse.web.servlet.SessionConfigurationInitializer;
import org.jmouse.web.servlet.SessionProperties;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

import java.util.List;
import java.util.Map;
import java.util.Set;

@BeanFactories
@BeanConditionIfProperty(name = "jmouse.web.enable", value = "true")
@BeanConditionExpression(value = "jmouse.web.enable eq 'true'")
public class ServletDispatcherConfiguration implements BeanContextAware {

    public static final String CONTEXT_PREFIX = "Context";

    @EnvironmentValue("jmouse.web.server.dispatcher.loadOnStartup")
    private int defaultLoadOnStartup;

    @EnvironmentValue("jmouse.web.server.dispatcher.mappings")
    private String[] defaultMapping;

    @EnvironmentValue("jmouse.web.server.default")
    private WebServers webServers;

    @EnvironmentValue("jmouse.web.server.dispatcher")
    private Map<String, Object> defaultConfig;

    private BeanContext context;

    public void setWebServers(WebServers webServers) {
        this.webServers = webServers;
    }

    public void setDefaultConfig(Map<String, Object> defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public void setDefaultLoadOnStartup(int defaultLoadOnStartup) {
        this.defaultLoadOnStartup = defaultLoadOnStartup;
    }

    public void setDefaultMapping(String[] defaultMapping) {
        this.defaultMapping = defaultMapping;
    }

    @Bean
    public ServletContextManager servletContextManager(WebBeanContext rootContext) {
        return new ServletContextManager(rootContext);
    }

    @Bean(proxied = true)
    @BeanConditionExpression(value = "'jMouse_Hello' | upper", expected = "JMOUSE", operator = ComparisonOperator.STARTS)
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
