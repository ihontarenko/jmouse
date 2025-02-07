package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.core.env.*;
import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.util.helper.Strings;

import java.nio.file.FileSystems;

import static org.jmouse.util.helper.Files.removeExtension;

abstract public class AbstractApplicationFactory<T extends BeanContext> implements ApplicationFactory<T> {

    public static final String SYSTEM_PROPERTIES     = "system-properties";
    public static final String SYSTEM_ENV_PROPERTIES = "system-env";

    protected PatternMatcherResourceLoader resourceLoader = new CompositeResourceLoader();

    @Override
    public Environment createDefaultEnvironment() {
        Environment environment = new StandardEnvironment();

        environment.addPropertySource(new SystemEnvironmentPropertySource(SYSTEM_ENV_PROPERTIES));
        environment.addPropertySource(new SystemPropertiesPropertySource(SYSTEM_PROPERTIES));

        loadApplicationProperties("classpath:package.properties", environment);
        loadApplicationProperties("classpath:jmouse-application.properties", environment);

        return environment;
    }

    protected void loadApplicationProperties(String location, Environment environment) {
        int counter = 0;
        for (Resource resource : resourceLoader.findResources(location)) {
            String name      = Strings.suffix(resource.getName(), FileSystems.getDefault().getSeparator(), true, 1);
            String formatted = "%s[%d]".formatted(removeExtension(name), counter++);
            environment.addPropertySource(new ResourcePropertySource(formatted, resource));
        }
    }

}
