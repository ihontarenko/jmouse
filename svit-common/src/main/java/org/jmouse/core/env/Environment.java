package org.jmouse.core.env;

/**
 * Central interface for accessing properties and resolving placeholders.
 */
public interface Environment extends PropertyResolver, PlaceholderResolver {

    /**
     * Returns the active profiles for the current environment.
     */
    String[] getDefaultProfiles();

    /**
     * Returns the default profiles for the current environment.
     */
    String[] getActiveProfiles();

    /**
     * Checks if a specific profile is active.
     */
    boolean acceptsProfile(String profile);

}
