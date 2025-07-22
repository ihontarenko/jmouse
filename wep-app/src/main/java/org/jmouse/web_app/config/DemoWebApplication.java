package org.jmouse.web_app.config;

import app.api.WebConfig;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.mvc.FrameworkDispatcherRegistration;
import org.jmouse.mvc.ServletDispatcherProperties;
import org.jmouse.mvc.WebBeanContextConfigurer;
import org.jmouse.mvc.context.BeanConditionExists;
import org.jmouse.mvc.jMouseWebMvcRoot;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.jMouseWebRoot;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.server.tomcat.TomcatWebServerFactory;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;
import org.jmouse.web_app.service.Service;

@BeanFactories
public class DemoWebApplication {

    @Bean("service")
    public Service getService() {
        return new Service("local_app");
    }

    @Bean("webServerFactory")
    public WebServerFactory createWebServerFactory() {
        return new TomcatWebServerFactory() {};
    }

    @Bean
    public DirectRequestPathMapping.Registration registration() {
        return new DirectRequestPathMapping.Registration("/app", (request, response)
                -> response.getWriter().write("web_app"));
    }

    @Bean(proxied = true)
    public ServletRegistrationBean<?> apiDispatcher(WebBeanContext rootContext) {

        WebBeanContext webBeanContext = (WebBeanContext) rootContext.getBean(ApplicationFactory.class)
                .createContext("apiContext", rootContext, WebConfig.class);

        webBeanContext.addBaseClasses(jMouseWebMvcRoot.class);

        WebBeanContextConfigurer configurer = webBeanContext.getBean(WebBeanContextConfigurer.class);

        configurer.webmvcInitializers(webBeanContext);

        webBeanContext.refresh();

        webBeanContext.setBaseClasses(WebConfig.class);

        configurer.defaultInitializers(webBeanContext);

        webBeanContext.refresh();

        ServletRegistrationBean<?> registration = new FrameworkDispatcherRegistration("apiDispatcher", webBeanContext);

        registration.setEnabled(true);
        registration.setLoadOnStartup(2);
        registration.addMappings("/api/*");

        return registration;
    }

}
