package org.jmouse.web.factories;

import org.jmouse.beans.annotation.SuppressException;
import org.jmouse.beans.definition.DuplicateBeanDefinitionException;
import org.jmouse.core.env.Environment;
import org.jmouse.core.env.MapPropertySource;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.context.ApplicationConfigurer;

import java.util.Map;

@BeanFactories
@SuppressException(DuplicateBeanDefinitionException.class)
public class WebComponentsApplicationConfigurer implements ApplicationConfigurer {

    @Override
    public void configureEnvironment(Environment environment) {
        System.out.println("configureEnvironment!!!!!!!!!!!!!!!!!!!!");
        environment.addPropertySource(new MapPropertySource("runtime", Map.of("jmouse.name", "jMouse - Web Framework")));
    }

}

