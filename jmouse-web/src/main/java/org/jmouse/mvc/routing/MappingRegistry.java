package org.jmouse.mvc.routing;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🗂️ Registry for mappings of type {@code M} to their registrations of type {@code T}.
 *
 * <p>Supports basic CRUD operations over mappings.</p>
 *
 * @param <M> the mapping key type
 * @param <T> the mapping registration type
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class MappingRegistry<T, M> {

    private final Map<M, MappingRegistration<T, M>> registry = new LinkedHashMap<>();

    /**
     * Registers a new mapping with its registration.
     *
     * @param mapping the mapping key
     * @param registration the registration associated with the mapping
     * @return the previous registration associated with the mapping, or {@code null} if none
     */
    public MappingRegistration<T, M> register(M mapping, MappingRegistration<T, M> registration) {
        MappingRegistration<?, ?> previous = getRegistration(mapping);

        if (previous != null) {
            throw new IllegalStateException(
                    "AMBIGUOUS MAPPING! Cannot register mapping %s there is already exists!".formatted(mapping));
        }

        return registry.put(mapping, registration);
    }

    /**
     * Retrieves the registration associated with the given mapping.
     *
     * @param mapping the mapping key
     * @return the associated registration, or {@code null} if not found
     */
    public MappingRegistration<T, M> getRegistration(M mapping) {
        return registry.get(mapping);
    }

    /**
     * Removes the registration associated with the given mapping.
     *
     * @param mapping the mapping key to remove
     * @return the removed registration, or {@code null} if none existed
     */
    public MappingRegistration<T, M> remove(M mapping) {
        return registry.remove(mapping);
    }

    /**
     * Checks if the registry contains the given mapping.
     *
     * @param mapping the mapping key to check
     * @return {@code true} if the mapping exists, {@code false} otherwise
     */
    public boolean contains(M mapping) {
        return registry.containsKey(mapping);
    }

    /**
     * Returns all registrations in the registry in insertion order.
     *
     * @return a collection of all mapping registrations
     */
    public Collection<MappingRegistration<T, M>> getRegistrations() {
        return registry.values();
    }

    /**
     * Returns all mappings in the registry in insertion order.
     *
     * @return a collection of all mapping registrations
     */
    public Collection<M> getMappings() {
        return registry.keySet();
    }

    /**
     * Returns the number of registered mappings.
     *
     * @return the size of the registry
     */
    public int size() {
        return registry.size();
    }

    /**
     * Clears all mappings from the registry.
     */
    public void clear() {
        registry.clear();
    }

}
