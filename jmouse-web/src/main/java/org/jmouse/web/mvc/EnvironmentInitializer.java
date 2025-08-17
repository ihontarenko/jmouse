package org.jmouse.web.mvc;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.core.env.Environment;
import org.jmouse.core.env.MapPropertySource;

import java.util.Map;

@Bean
public class EnvironmentInitializer implements BeanConfigurer<Environment> {

    @Override
    public void configure(Environment environment) {
        environment.addPropertySource(
                new MapPropertySource("EnvironmentInitializer",
                                      Map.of("jmouse.web.name", "jMouse: class -> " + getClass().getName())
                )
        );
    }

}
