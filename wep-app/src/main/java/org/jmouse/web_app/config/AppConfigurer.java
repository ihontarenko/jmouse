package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.core.env.Environment;
import org.jmouse.core.env.MapPropertySource;

import java.util.HashMap;

@BeanFactories
public class AppConfigurer implements ApplicationConfigurer {

    @Override
    public void configureEnvironment(Environment environment) {
        environment.addPropertySource(new MapPropertySource("webapp", new HashMap<>() {{
            put("webapp", getClass().getName());
        }}));
    }
}
