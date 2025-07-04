package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.mvc._test.Service;

@BeanFactories
public class DemoWebApplication {

    @Bean("service")
    public Service getService() {
        return new Service("local_app");
    }

}
