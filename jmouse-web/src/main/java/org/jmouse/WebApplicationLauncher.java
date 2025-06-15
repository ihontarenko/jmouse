package org.jmouse;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.BindDefault;
import org.jmouse.core.env.Environment;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.util.IdGenerator;
import org.jmouse.util.SimpleRandomStringGenerator;
import org.jmouse.util.SingletonSupplier;
import org.jmouse.web.WebApplicationFactory;
import org.jmouse.web.servlet.SessionProperties;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.initializer.application.WebApplicationInitializerProvider;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.mvc.FrameworkDispatcherServlet;
import org.jmouse.mvc.FrameworkDispatcherServletRegistration;
import org.jmouse.web.servlet.SessionConfigurationInitializer;
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
        scannerBeanContextInitializer.addScanner(
                rootTypes -> new ArrayList<>(ClassFinder.findImplementations(ApplicationConfigurer.class, rootTypes)));
        scannerBeanContextInitializer.addScanner(
                rootTypes -> new ArrayList<>(ClassFinder.findAnnotatedClasses(BeanProperties.class, rootTypes)));

        return scannerBeanContextInitializer;
    }

    public WebBeanContext launch() {
        ApplicationFactory<WebBeanContext> applicationFactory = new WebApplicationFactory();
        Environment                        environment        = applicationFactory.createDefaultEnvironment();
        WebBeanContext                     rootContext        = applicationFactory.createContext(
                WebBeanContext.DEFAULT_ROOT_WEB_CONTEXT_NAME, baseClasses);

        ScannerBeanContextInitializer scannerBeanContextInitializer = getScannerBeanContextInitializer();
        rootContext.addInitializer(scannerBeanContextInitializer);
        rootContext.addInitializer(new StartupRootApplicationContextInitializer(environment));
        rootContext.refresh();

        // attach ApplicationFactory object
        rootContext.registerBean(ApplicationFactory.class, applicationFactory);

        IdGenerator<String> idGenerator = new SimpleRandomStringGenerator(11);

        rootContext.registerBean("s1", idGenerator::generate, BeanScope.REQUEST);
        rootContext.registerBean("s2", idGenerator::generate, BeanScope.SESSION);

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

    @Configuration
    public static class ServletDispatcherConfiguration {

        @Provide("defaultDispatcherContext")
        public WebBeanContext webDefaultBeanContext(WebBeanContext rootContext) {
            ApplicationFactory<WebBeanContext> factory        = rootContext.getBean(ApplicationFactory.class);
            WebBeanContext                     webBeanContext = factory.createContext(
                    "DEFAULT_DISPATCHER_CONTEXT", rootContext);

            webBeanContext.registerBean("dispatcher", "DEFAULT DISPATCHER!!!");

            return webBeanContext;
        }

        @Provide("indexDispatcherContext")
        public WebBeanContext indexDispatcherContext(WebBeanContext rootContext) {
            ApplicationFactory<WebBeanContext> factory        = rootContext.getBean(ApplicationFactory.class);
            WebBeanContext                     webBeanContext = factory.createContext(
                    "INDEX_DISPATCHER_CONTEXT", rootContext);

            webBeanContext.registerBean("dispatcher", "index dispatcher!!!");

            return webBeanContext;
        }

        @Provide
        public ServletRegistrationBean<?> defaultDispatcher(
                @Qualifier(WebBeanContext.DEFAULT_ROOT_WEB_CONTEXT_NAME) WebBeanContext rootContext,
                @Qualifier("defaultDispatcherContext") WebBeanContext webBeanContext,
                DispatcherProperties properties) {
            ServletRegistrationBean<?> registration = new FrameworkDispatcherServletRegistration(webBeanContext);

            registration.setEnabled(properties.isEnabled());
            registration.setLoadOnStartup(properties.getLoadOnStartup());
            registration.addMappings(properties.getMappings());

            return registration;
        }

        @Provide
        public ServletRegistrationBean<?> indexDispatcher(
                WebBeanContext rootContext,
                @Qualifier("indexDispatcherContext") WebBeanContext webBeanContext,
                DispatcherProperties properties) {
            ServletRegistrationBean<?> registration = new ServletRegistrationBean<>(
                    null, new FrameworkDispatcherServlet(webBeanContext));

            registration.setEnabled(properties.isEnabled());
            registration.setLoadOnStartup(properties.getLoadOnStartup() + 1);
            registration.addMappings("/index");

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
