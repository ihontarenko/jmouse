package org.jmouse.core.environment;

import org.jmouse.core.io.ClasspathResourceLoader;
import org.jmouse.core.io.Resource;

/**
 * A {@link ResourcePropertySource} that loads properties from a classpath resource.
 * <p>
 * This implementation resolves a resource located on the classpath and makes its properties
 * available as a property source.
 * </p>
 */
public class ClasspathPropertySource extends ResourcePropertySource {

    /**
     * Creates a new {@code ClasspathPropertySource} with the given name and classpath location.
     *
     * @param name     the name of the property source
     * @param location the classpath location of the properties file
     */
    public ClasspathPropertySource(String name, String location) {
        super(name, loadResource(location));
    }

    /**
     * Loads a resource from the classpath.
     *
     * @param location the classpath location of the resource
     * @return the resolved {@link Resource}
     */
    private static Resource loadResource(String location) {
        return new ClasspathResourceLoader().getResource(location);
    }
}
