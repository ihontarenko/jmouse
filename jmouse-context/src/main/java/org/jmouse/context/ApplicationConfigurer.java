package org.jmouse.context;

import org.jmouse.core.convert.ConversionConfigurer;

/**
 * A high-level configuration interface for setting up an application context. By extending
 * both  {@link ConversionConfigurer}, implementations can
 * configure mappings, conversions, and optionally register singletons within a
 *
 * @see ConversionConfigurer
 */
public interface ApplicationConfigurer extends ServiceConfigurer {

}
