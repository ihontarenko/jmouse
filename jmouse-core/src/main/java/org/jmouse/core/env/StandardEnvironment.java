package org.jmouse.core.env;

import org.jmouse.util.helper.Arrays;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A standard implementation of {@link Environment} that provides access to property resolution
 * and profile management.
 * <p>
 * This class delegates property resolution to an internal {@link PropertyResolver} and
 * maintains active profiles that can influence application behavior.
 * </p>
 */
public class StandardEnvironment implements Environment {

    private final PropertyResolver resolver;
    private final Set<String> profiles = new HashSet<>();

    /**
     * Creates a {@code StandardEnvironment} with a default {@link StandardPropertyResolver}.
     */
    public StandardEnvironment() {
        this(new StandardPropertyResolver());
    }

    /**
     * Creates a {@code StandardEnvironment} with a custom {@link PropertyResolver}.
     *
     * @param resolver the property resolver to use
     */
    public StandardEnvironment(PropertyResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Retrieves the default profiles for the environment.
     * <p>
     * DirectAccess profiles are used when no active profiles are explicitly set.
     * </p>
     *
     * @return an array of default profile names
     */
    @Override
    public String[] getDefaultProfiles() {
        Set<String> defaultProfiles = new HashSet<>();
        // todo:
        return defaultProfiles.toArray(String[]::new);
    }

    /**
     * Retrieves the active profiles for the environment.
     * <p>
     * If no active profiles are explicitly set, this method attempts to resolve them
     * from the {@code profiles} property.
     * </p>
     *
     * @return an array of active profile names
     */
    @Override
    public String[] getActiveProfiles() {
        String[] activeProfiles = profiles.toArray(String[]::new);

        if (Arrays.empty(activeProfiles)) {
            String profiles = getProperty("profiles");

            if (profiles != null) {
                activeProfiles = profiles.split(",");
                this.profiles.addAll(List.of(activeProfiles));
            }
        }

        return activeProfiles;
    }

    /**
     * Determines whether the given profile is currently active.
     *
     * @param profile the profile name to check
     * @return {@code true} if the profile is active, {@code false} otherwise
     */
    @Override
    public boolean acceptsProfile(String profile) {
        return Set.of(getActiveProfiles()).contains(profile);
    }

    /**
     * Retrieves the raw property value without type conversion.
     *
     * @param name the property name
     * @return the raw property value, or {@code null} if not found
     */
    @Override
    public Object getRawProperty(String name) {
        return resolver.getRawProperty(name);
    }

    /**
     * Retrieves the property value as the specified type.
     *
     * @param name       the property name
     * @param targetType the target type to convert the property value to
     * @param <T>        the expected type of the property value
     * @return the converted property value, or {@code null} if not found
     */
    @Override
    public <T> T getProperty(String name, Class<T> targetType) {
        return resolver.getProperty(name, targetType);
    }

    /**
     * Retrieves a {@link PropertySource} by its name.
     *
     * @param name the name of the property source
     * @return the corresponding {@link PropertySource}, or {@code null} if not found
     */
    @Override
    public PropertySource<?> getPropertySource(String name) {
        return resolver.getPropertySource(name);
    }

    /**
     * Adds a new {@link PropertySource} to the environment.
     *
     * @param propertySource the property source to add
     */
    @Override
    public void addPropertySource(PropertySource<?> propertySource) {
        resolver.addPropertySource(propertySource);
    }

    /**
     * Checks whether a property source with the given name exists.
     *
     * @param name the name of the property source
     * @return {@code true} if the property source exists, {@code false} otherwise
     */
    @Override
    public boolean hasPropertySource(String name) {
        return resolver.hasPropertySource(name);
    }

    /**
     * Removes a {@link PropertySource} by its name.
     *
     * @param name the name of the property source
     * @return {@code true} if the property source was removed, {@code false} if not found
     */
    @Override
    public boolean removePropertySource(String name) {
        return resolver.removePropertySource(name);
    }

    /**
     * Retrieves all registered {@link PropertySource} instances.
     *
     * @return a collection of registered property sources
     */
    @Override
    public Collection<? extends PropertySource<?>> getPropertySources() {
        return resolver.getPropertySources();
    }
}
