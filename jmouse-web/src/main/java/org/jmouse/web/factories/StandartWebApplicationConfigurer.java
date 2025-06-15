package org.jmouse.web.factories;

import org.jmouse.beans.annotation.SuppressException;
import org.jmouse.beans.definition.DuplicateBeanDefinitionException;
import org.jmouse.core.env.Environment;
import org.jmouse.core.env.MapPropertySource;
import org.jmouse.beans.BeanContainer;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.context.ApplicationConfigurer;

import java.util.Map;

@Configuration
@SuppressException(DuplicateBeanDefinitionException.class)
public class StandartWebApplicationConfigurer implements ApplicationConfigurer {

    @Override
    public void configureEnvironment(Environment environment) {
        System.out.println("configureEnvironment!!!!!!!!!!!!!!!!!!!!");
        environment.addPropertySource(new MapPropertySource("runtime", Map.of("jmouse.name", "jMouse - Web Framework")));
    }

    /**
     * Registers any required singletons with the given {@link BeanContainer}. The default
     * implementation is a no-op; overriding implementations can add custom logic for
     * instantiating and wiring up singletons that should be globally available in
     * the application context.
     *
     * @param container the {@link BeanContainer} in which to register singletons
     */
    @Override
    public void registerSingleton(BeanContainer container) {
        container.registerBean("appName", "web-application");
    }
}

