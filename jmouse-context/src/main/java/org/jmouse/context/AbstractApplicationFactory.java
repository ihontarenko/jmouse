package org.jmouse.context;

import org.jmouse.core.env.*;
import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.util.Strings;

import java.nio.file.FileSystems;

import static org.jmouse.util.Files.removeExtension;

/**
 * Abstract base class for implementing {@link ApplicationFactory}.
 * <p>
 * Provides default implementations for creating an {@link Environment} and
 * loading application properties from classpath resources.
 * </p>
 *
 * @param <T> the type of {@link ApplicationBeanContext} managed by this factory
 */
abstract public class AbstractApplicationFactory<T extends ApplicationBeanContext> implements ApplicationFactory<T> {

    /** Property source name for system properties. */
    public static final String SYSTEM_PROPERTIES = "system-properties";

    /** Property source name for system environment variables. */
    public static final String SYSTEM_ENV_PROPERTIES = "system-env";

    /** A resource loader for scanning and loading application properties. */
    protected PatternMatcherResourceLoader resourceLoader = new CompositeResourceLoader();

    /**
     * Creates a default environment with system properties, environment variables,
     * and predefined application property files.
     *
     * @return a new {@link Environment} instance with default settings
     */
    @Override
    public Environment createDefaultEnvironment() {
        Environment environment = new StandardEnvironment();

        // Add system environment variables and system properties as property sources
        environment.addPropertySource(new SystemEnvironmentPropertySource(SYSTEM_ENV_PROPERTIES));
        environment.addPropertySource(new SystemPropertiesPropertySource(SYSTEM_PROPERTIES));

        // Load application properties from predefined locations
        loadApplicationProperties("classpath:package.properties", environment);
//        loadApplicationProperties("classpath:jmouse-application.properties", environment);
        loadApplicationProperties("classpath:*/jmouse*.properties", environment);

        // will fails
//        loadApplicationProperties("classpath:*/jmouse*.yaml", environment);

        return environment;
    }

    /**
     * Loads application properties from a given location and adds them as property sources.
     * <p>
     * Each discovered resource is assigned a unique name based on its filename and index.
     * </p>
     *
     * @param location    the resource location pattern to search for properties
     * @param environment the environment to which the properties should be added
     */
    protected void loadApplicationProperties(String location, Environment environment) {
        int counter = 0;
        for (Resource resource : resourceLoader.findResources(location)) {
            loadApplicationProperties(resource, environment, counter++);
        }
    }

    /**
     * Loads application properties from a single resource and registers it as a property source. 📦
     * <p>
     * Property-source name is derived from the resource filename (without extension)
     * and suffixed with the index: {@code <basename>[number]}.
     * </p>
     *
     * @param resource    the properties resource (e.g., .properties, .yml, .yaml)
     * @param environment target environment where the property source will be added
     * @param number      index used to disambiguate property-source names
     */
    protected void loadApplicationProperties(Resource resource, Environment environment, int number) {
        // Extract filename and remove extension for naming the property source
        String name = Strings.suffix(resource.getName(), FileSystems.getDefault().getSeparator(), true, 1);
        String formatted = "%s[%d]".formatted(removeExtension(name), number);
        // Add the resource as a property source
        environment.addPropertySource(new ResourcePropertySource(formatted, resource));
    }
}
