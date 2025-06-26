package org.jmouse.mvc._test;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.mvc.context.BeanConditionExists;

@BeanFactories
public class ServiceConfig {

    @Bean("service")
    @BeanConditionExists("service")
    public Service getService() {
        return new Service("core");
    }

}
