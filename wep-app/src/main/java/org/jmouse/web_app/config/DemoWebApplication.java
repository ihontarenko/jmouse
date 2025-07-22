package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.mvc.context.BeanConditionExists;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.server.tomcat.TomcatWebServerFactory;
import org.jmouse.web_app.service.Service;

@BeanFactories
public class DemoWebApplication {

    @Bean("service")
    public Service getService() {
        return new Service("local_app");
    }

    @Bean("webServerFactory")
    public WebServerFactory createWebServerFactory() {
        return new TomcatWebServerFactory();
    }

}
