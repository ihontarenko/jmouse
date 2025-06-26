package org.jmouse.mvc;

import org.jmouse.core.env.Environment;
import org.jmouse.core.env.MapPropertySource;

import java.util.Map;

public class EnvironmentInitializer implements BeanInstanceInitializer<Environment> {

    @Override
    public void initialize(Environment environment) {
        environment.addPropertySource(
                new MapPropertySource("EnvironmentInitializer",
                                      Map.of("jmouse.web.name", "jMouse: class -> " + getClass().getName())
                )
        );
    }

    @Override
    public Class<Environment> objectClass() {
        return Environment.class;
    }

}
