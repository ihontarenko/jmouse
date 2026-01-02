package org.jmouse.core.environment;

/**
 * A {@link PropertySource} implementation that uses system properties as the underlying property source.
 * <p>
 * This class extends {@link JavaPropertiesPropertySource}, initializing the property source with
 * {@link System#getProperties()} and assigning a default order value for priority.
 * </p>
 */
public class SystemPropertiesPropertySource extends JavaPropertiesPropertySource {

    /**
     * Constructs a new {@link SystemPropertiesPropertySource} with the specified name.
     *
     * @param name the name of the property source
     */
    public SystemPropertiesPropertySource(String name) {
        super(name, System.getProperties());
        setOrder(Integer.MAX_VALUE >> 2); // Assigns a default order with lower priority
    }
}
