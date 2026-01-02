package org.jmouse.core.environment;

import org.jmouse.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * A {@link JavaPropertiesPropertySource} that loads properties from a {@link Resource}.
 * <p>
 * This implementation reads properties from the specified resource and makes them available
 * as a property source.
 * </p>
 */
public class ResourcePropertySource extends JavaPropertiesPropertySource {

    private final static Logger LOGGER = LoggerFactory.getLogger(ResourcePropertySource.class);

    /**
     * Creates a new {@code ResourcePropertySource} with the given name and resource.
     *
     * @param name     the name of the property source
     * @param resource the resource containing properties
     */
    public ResourcePropertySource(String name, Resource resource) {
        super(name, loadJavaProperties(resource));
    }

    /**
     * Loads properties from the given resource.
     *
     * @param resource the resource to read properties from
     * @return a {@link Properties} structured populated with the resource's properties
     */
    private static Properties loadJavaProperties(Resource resource) {
        Properties properties = new Properties();

        try {
            properties.load(resource.getReader());
        } catch (IOException e) {
            LOGGER.error("Unable to load properties from: {}", resource, e);
        }

        return properties;
    }
}
