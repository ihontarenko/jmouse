package org.jmouse.web.mvc.resource;

import java.util.LinkedList;
import java.util.List;

/**
 * 📂 Registry for static resource handler registrations.
 *
 * <p>Allows registering {@link ResourceRegistration}s with
 * URL patterns and retrieving them later for mapping.</p>
 */
public class ResourceHandlerRegistry {

    /**
     * 📋 Registered resource handler entries.
     */
    private final List<ResourceRegistration> registrations = new LinkedList<>();

    /**
     * ➕ Register a new static resource handler for the given patterns.
     *
     * @param patterns ant-style URL patterns (e.g. {@code /static/**})
     * @return created {@link ResourceRegistration}
     */
    public ResourceRegistration registerHandler(String... patterns) {
        ResourceRegistration registration = new ResourceRegistration();
        registrations.add(registration.addPatterns(patterns));
        return registration;
    }

    /**
     * 📑 Get all registered resource handlers.
     *
     * @return list of {@link ResourceRegistration}s
     */
    public List<ResourceRegistration> getRegistrations() {
        return registrations;
    }
}
