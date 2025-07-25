package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.mvc.BeanConfigurer;
import org.jmouse.mvc.HandlerInterceptorRegistry;
import org.jmouse.mvc.interceptor.LoggingHandlerInterceptor;

@Bean
public class HandlerInterceptorRegistryConfigurer implements BeanConfigurer<HandlerInterceptorRegistry> {

    /**
     * 🔧 Apply additional configuration to the target object.
     *
     * @param registry instance to initialize
     */
    @Override
    public void configure(HandlerInterceptorRegistry registry) {
        registry.addInterceptor(new LoggingHandlerInterceptor());
    }

}
