package org.jmouse.web_app.config;

import app.api.WebConfig;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.mvc.FrameworkDispatcherRegistration;
import org.jmouse.mvc.Route;
import org.jmouse.mvc.mapping.ControllerMethodRegistration;
import org.jmouse.web.WebContextBuilder;
import org.jmouse.web.context.WebBeanContext;
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
    public ControllerMethodRegistration registration() {
        return new ControllerMethodRegistration(Route.GET("/app"), (request, response)
                -> response.getWriter().write("web_app"));
    }

    @Bean(proxied = true)
    public ServletRegistrationBean<?> apiDispatcher(WebBeanContext rootContext, WebContextBuilder builder) {
        WebBeanContext context = builder.name("api-ctx")
                .baseClasses(WebConfig.class).parent(rootContext)
                .useWebMvc().useDefault()
                .build();

        ServletRegistrationBean<?> registration = new FrameworkDispatcherRegistration("apiDispatcher", context);

        registration.setEnabled(true);
        registration.setLoadOnStartup(2);
        registration.addMappings("/api/*");

        return registration;
    }

}
