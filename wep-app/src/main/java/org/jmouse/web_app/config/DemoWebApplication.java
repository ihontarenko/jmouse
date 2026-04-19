package org.jmouse.web_app.config;

import app.api.WebConfig;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.jdbc.connection.datasource.DataSourceContributor;
import org.jmouse.jdbc.connection.datasource.DataSourceSpecification;
import org.jmouse.jdbc.connection.datasource.DataSourceSpecificationRegistry;
import org.jmouse.web.mvc.FrameworkDispatcherRegistration;
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

    @Bean(proxied = true)
    public ServletRegistrationBean<?> apiDispatcher(WebBeanContext rootContext, WebContextBuilder builder) {
        WebBeanContext context = builder.name("api-ctx")
                .userClasses(WebConfig.class).parent(rootContext)
                .useWebMvc().useDefault()
                .build();

        ServletRegistrationBean<?> registration = new FrameworkDispatcherRegistration("apiDispatcher", context);

        registration.setEnabled(true);
        registration.setLoadOnStartup(2);
        registration.addMappings("/api/*");

        return registration;
    }

    @Bean
    public DataSourceContributor dataSourceContributor() {
        return registry -> registry.register(new DataSourceSpecification(
                "mysql",
                null,
                "jdbc:mysql://127.0.0.1:3306/jmouse?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC",
                "jmouse",
                "jmouse",
                "jmouse",
                "mysql",
                null,
                null
        ));
    }

}
