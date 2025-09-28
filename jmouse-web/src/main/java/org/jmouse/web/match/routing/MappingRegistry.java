package org.jmouse.web.match.routing;

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
public final class MappingRegistry<T> {

    private final Map<MappingCriteria, MappingRegistration<T>> registry = new LinkedHashMap<>();

    /**
     * Registers a new mapping with its registration.
     *
     * @param criteria the mapping key
     * @param registration the registration associated with the mapping
     * @return the previous registration associated with the mapping, or {@code null} if none
     */
    public MappingRegistration<T> register(MappingCriteria criteria, MappingRegistration<T> registration) {
        MappingRegistration<?> previous = getRegistration(criteria);

        if (previous != null) {
            throw new IllegalStateException(
                    "AMBIGUOUS MAPPING! Cannot register mapping-criteria %s there is already exists!".formatted(criteria));
        }

        return registry.put(criteria, registration);
    }

    /**
     * Retrieves the registration associated with the given mapping.
     *
     * @param mapping the mapping key
     * @return the associated registration, or {@code null} if not found
     */
    public MappingRegistration<T> getRegistration(MappingCriteria mapping) {
        return registry.get(mapping);
    }

    /**
     * Removes the registration associated with the given mapping.
     *
     * @param mapping the mapping key to remove
     * @return the removed registration, or {@code null} if none existed
     */
    public MappingRegistration<T> remove(MappingCriteria criteria) {
        return registry.remove(criteria);
    }

    /**
     * Checks if the registry contains the given mapping.
     *
     * @param mapping the mapping key to check
     * @return {@code true} if the mapping exists, {@code false} otherwise
     */
    public boolean contains(MappingCriteria criteria) {
        return registry.containsKey(criteria);
    }

    /**
     * Returns all registrations in the registry in insertion order.
     *
     * @return a collection of all mapping registrations
     */
    public Collection<MappingRegistration<T>> getRegistrations() {
        return registry.values();
    }

    /**
     * Returns all mappings in the registry in insertion order.
     *
     * @return a collection of all mapping registrations
     */
    public Collection<MappingCriteria> getMappingCriteria() {
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
