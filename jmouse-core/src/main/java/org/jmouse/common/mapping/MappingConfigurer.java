package org.jmouse.common.mapping;

/**
 * A simple hook for configuring or extending a {@link Mapping} instance with
 * custom logic or additional mappings. Implementations can override
 * {@link #configureMapping(Mapping)} to register new mappings, set defaults,
 * or otherwise influence the mapping process.
 *
 * @see Mapping
 */
public interface MappingConfigurer {

    /**
     * Configures the provided {@link Mapping} instance. By default, this method
     * does nothing. Implementing classes should override it to add or modify mappings,
     * set default behaviors, or perform any other necessary setup.
     *
     * @param mapping the {@link Mapping} to be configured
     */
    default void configureMapping(Mapping mapping) {
        // no-op
    }

}
