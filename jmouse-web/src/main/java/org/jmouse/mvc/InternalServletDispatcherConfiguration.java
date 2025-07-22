package org.jmouse.mvc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.context.BeanConditionIfProperty;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.BindDefault;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

@BeanFactories
@BeanConditionIfProperty(name = "jmouse.web.server.internal.dispatcher.enabled", value = "true")
public class InternalServletDispatcherConfiguration implements BeanContextAware {

    public static final String CONTEXT_PREFIX = "Context";

    private BeanContext context;

    @Bean(proxied = true)
    public ServletRegistrationBean<?> internalDispatcher(
            ServletContextManager servletContextManager, Properties properties) {
        WebBeanContext dispatcherContext = servletContextManager.createServletDispatcherContext(
                properties.getName() + CONTEXT_PREFIX, getBeanContext().getBaseClasses());
        ServletRegistrationBean<?> registration = new FrameworkDispatcherRegistration(
                properties.getName(), dispatcherContext);

        registration.setEnabled(properties.isEnabled());
        registration.setLoadOnStartup(properties.getLoadOnStartup());
        registration.addMappings(properties.getMappings());

        return registration;
    }

    @Bean(proxied = true, value = "internalIndex")
    public DirectRequestPathMapping.Registration registrationA() {
        return new DirectRequestPathMapping.Registration("/index", (request, response)
                -> response.getWriter().write("index"));
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

    @BeanProperties("jmouse.web.server.internal.dispatcher")
    public static class Properties {

        private String[] mappings;
        private int      loadOnStartup;
        private boolean  enabled;
        private String   name = "default";

        public boolean isEnabled() {
            return enabled;
        }

        @BindDefault("true")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String[] getMappings() {
            return mappings;
        }

        @BindDefault("/*")
        public void setMappings(String[] mappings) {
            this.mappings = mappings;
        }

        public int getLoadOnStartup() {
            return loadOnStartup;
        }

        @BindDefault("1")
        public void setLoadOnStartup(int loadOnStartup) {
            this.loadOnStartup = loadOnStartup;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

}
