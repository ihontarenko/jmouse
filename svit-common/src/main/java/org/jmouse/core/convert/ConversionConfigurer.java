package org.jmouse.core.convert;

/**
 * A simple hook for configuring or extending a {@link Conversion} instance with
 * custom logic or additional converters. Implementations can override
 * {@link #configureConversion(Conversion)} to register new type converters,
 * set default options, or otherwise influence the conversion process.
 *
 * @see DefaultConversion
 */
public interface ConversionConfigurer {

    /**
     * Configures the provided {@link Conversion} instance. By default, this method
     * is a no-op. Subclasses or implementing classes should override it to add or
     * modify converters, set default options, or perform any other necessary setup.
     *
     * @param conversion the {@link Conversion} to be configured
     */
    default void configureConversion(Conversion conversion) {
        // no-op
    }

}
