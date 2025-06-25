package org.jmouse.mvc;

import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Factories;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.context.BeanConstraintIfProperty;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.BindDefault;
import org.jmouse.core.env.Environment;
import org.jmouse.jMouseWebRoot;
import org.jmouse.mvc.context.CoreScannerBeanContextInitializer;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;
import org.jmouse.util.SingletonSupplier;
import org.jmouse.web.WebApplicationFactory;
import org.jmouse.web.servlet.SessionProperties;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.initializer.WebApplicationInitializerProvider;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.servlet.SessionConfigurationInitializer;
import org.jmouse.web.servlet.registration.RegistrationBean;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

import java.util.List;
import java.util.Set;

public class WebApplicationLauncher {

    private final Class<?>[] baseClasses;

    public WebApplicationLauncher(Class<?>... baseClasses) {
        this.baseClasses = baseClasses;
    }

    public WebBeanContext launch() {
        ApplicationFactory<WebBeanContext> applicationFactory = new WebApplicationFactory();
        Environment                        environment        = applicationFactory.createDefaultEnvironment();
        WebBeanContext                     rootContext        = applicationFactory.createContext(
                WebBeanContext.DEFAULT_ROOT_WEB_CONTEXT_NAME, baseClasses);

        rootContext.setBaseClasses(jMouseWebRoot.class);

        rootContext.registerBean(Environment.class, environment);

        rootContext.addInitializer(new CoreScannerBeanContextInitializer());
        rootContext.addInitializer(new StartupRootApplicationContextInitializer(environment));
        rootContext.refresh();

        // attach ApplicationFactory object
        rootContext.registerBean(ApplicationFactory.class, applicationFactory);

        // web server part
        createWebServer(rootContext).start();

        return rootContext;
    }

    public WebServer createWebServer(WebBeanContext rootContext) {
        List<WebApplicationInitializer> initializers = new WebApplicationInitializerProvider(
                rootContext).getExcluding(RegistrationBean.class);

        WebServerFactory factory = rootContext.getBean(WebServerFactory.class);

        return factory.getWebServer(initializers.toArray(WebApplicationInitializer[]::new));
    }


    @Factories
    @BeanConstraintIfProperty(name = "application", value = "jMouse", operator = BeanConstraintIfProperty.ComparisonOperator.CONTAINS)
    public static class ServletDispatcherConfiguration {

        @Provide("requestPathMappingRegistrations")
        public List<DirectRequestPathMapping.Registration> requestPathMappingRegistrations(
                @AggregatedBeans Set<DirectRequestPathMapping.Registration> registrations) {
            return List.copyOf(registrations);
        }

        @Provide("defaultDispatcherContext")
        public WebBeanContext webDefaultBeanContext(WebBeanContext rootContext) {
            ApplicationFactory<WebBeanContext> factory        = rootContext.getBean(ApplicationFactory.class);
            WebBeanContext                     webBeanContext = factory.createContext(
                    "DEFAULT_DISPATCHER_CONTEXT", rootContext);

            webBeanContext.registerBean("dispatcher", "DEFAULT DISPATCHER!!!");

            return webBeanContext;
        }

        @Provide
        public ServletRegistrationBean<?> defaultDispatcher(
                @Qualifier("defaultDispatcherContext") WebBeanContext webBeanContext,
                DispatcherProperties properties) {
            ServletRegistrationBean<?> registration = new FrameworkDispatcherRegistration(webBeanContext);

            registration.setEnabled(properties.isEnabled());
            registration.setLoadOnStartup(properties.getLoadOnStartup());
            registration.addMappings(properties.getMappings());

            return registration;
        }

        @Provide
        public SessionConfigurationInitializer sessionInitializer(WebBeanContext rootContext) {
            return new SessionConfigurationInitializer(
                    SingletonSupplier.of(() -> rootContext.getBean(SessionProperties.class)));
        }

        @BeanProperties("jmouse.web.server.dispatcher")
        public static class DispatcherProperties {

            private String[] mappings;
            private int      loadOnStartup;
            private boolean  enabled;

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

        }

    }

}
