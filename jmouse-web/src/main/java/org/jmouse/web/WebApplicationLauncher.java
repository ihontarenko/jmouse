package org.jmouse.web;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.Bind;
import org.jmouse.core.env.Environment;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.util.SingletonSupplier;
import org.jmouse.web.configuration.SessionProperties;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.initializer.application.WebApplicationInitializerProvider;
import org.jmouse.web.initializer.application.jMouseWebApplicationInitializer;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerConfigHolder;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.servlet.FrameworkDispatcherServlet;
import org.jmouse.web.servlet.FrameworkDispatcherServletRegistration;
import org.jmouse.web.servlet.SessionCookieInitializer;
import org.jmouse.web.servlet.registration.RegistrationBean;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

import java.util.ArrayList;
import java.util.List;

public class WebApplicationLauncher {

    private final Class<?>[] baseClasses;

    public WebApplicationLauncher(Class<?>... baseClasses) {
        this.baseClasses = baseClasses;
    }

    private static ScannerBeanContextInitializer getScannerBeanContextInitializer() {
        ScannerBeanContextInitializer scannerBeanContextInitializer = new ScannerBeanContextInitializer();

        scannerBeanContextInitializer.addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findImplementations(WebApplicationInitializer.class, rootTypes)));
        scannerBeanContextInitializer.addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findImplementations(ApplicationConfigurer.class, rootTypes)));
        scannerBeanContextInitializer.addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findAnnotatedClasses(BeanProperties.class, rootTypes)));

        return scannerBeanContextInitializer;
    }

    public WebBeanContext launch() {
        ApplicationFactory<WebBeanContext> applicationFactory = new WebApplicationFactory();
        Environment                        environment        = applicationFactory.createDefaultEnvironment();

        ScannerBeanContextInitializer scannerBeanContextInitializer = getScannerBeanContextInitializer();

        WebBeanContext rootContext = applicationFactory.createContext(
                WebBeanContext.DEFAULT_ROOT_WEB_CONTEXT_NAME, baseClasses);

        rootContext.addInitializer(scannerBeanContextInitializer);
        rootContext.addInitializer(new StartupRootApplicationContextInitializer(environment));
        rootContext.refresh();

        // attach ApplicationFactory object
        rootContext.registerBean(ApplicationFactory.class, applicationFactory);

        SessionProperties properties = rootContext.getBean(SessionProperties.class);

        // web server part
        createWebServer(rootContext).start();

        return rootContext;
    }

    public WebServer createWebServer(WebBeanContext rootContext) {
        WebServerConfigHolder           webServerConfigHolder = new WebServerConfigHolder();
        List<WebApplicationInitializer> initializers          = new WebApplicationInitializerProvider(
                rootContext).getWebApplicationInitializersExcluding(RegistrationBean.class);

        Bind.with(rootContext.getEnvironmentBinder())
                .to(WebServerFactory.WEB_SERVER_CONFIGURATION_PATH, webServerConfigHolder);

        WebServerFactory factory = rootContext.getBean(WebServerFactory.class);
        return factory.getWebServer(initializers.toArray(WebApplicationInitializer[]::new));
    }

    @Configuration
    public static class ServletDispatcherConfiguration {

        @Provide
        public ServletRegistrationBean<?> defaultDispatcher(WebBeanContext rootContext, DispatcherProperties properties) {
            ServletRegistrationBean<?> registration = new FrameworkDispatcherServletRegistration(rootContext);

            registration.setEnabled(properties.isEnabled());
            registration.setLoadOnStartup(properties.getLoadOnStartup());
            registration.addMappings(properties.getMappings());

            return registration;
        }

        @Provide
        public ServletRegistrationBean<?> indexDispatcher(WebBeanContext rootContext, DispatcherProperties properties) {
            ServletRegistrationBean<?> registration = new ServletRegistrationBean<>(null, new FrameworkDispatcherServlet(rootContext));

            registration.setEnabled(properties.isEnabled());
            registration.setLoadOnStartup(properties.getLoadOnStartup() + 1);
            registration.addMappings("/index");

            return registration;
        }

        @Provide
        public SessionCookieInitializer sessionInitializer(WebBeanContext rootContext) {
            return new SessionCookieInitializer(SingletonSupplier.of(() -> rootContext.getBean(SessionProperties.class)));
        }

        @BeanProperties("jmouse.web.server.dispatcher")
        public static class DispatcherProperties {

            private String[] mappings;
            private int      loadOnStartup;
            private boolean  enabled;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String[] getMappings() {
                return mappings;
            }

            public void setMappings(String[] mappings) {
                this.mappings = mappings;
            }

            public int getLoadOnStartup() {
                return loadOnStartup;
            }

            public void setLoadOnStartup(int loadOnStartup) {
                this.loadOnStartup = loadOnStartup;
            }

        }

    }

}
