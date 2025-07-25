package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.mvc.BeanConfigurer;

@Bean
public class AppBeanInstanceInitializer implements BeanConfigurer<DemoWebApplication> {
    /**
     * 🔧 Apply additional configuration to the target object.
     *
     * @param object instance to initialize
     */
    @Override
    public void configure(DemoWebApplication object) {
        System.out.println(object);
    }
}
