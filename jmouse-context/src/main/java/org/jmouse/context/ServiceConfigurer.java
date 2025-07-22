package org.jmouse.context;

import org.jmouse.core.convert.ConversionConfigurer;
import org.jmouse.core.env.EnvironmentConfigurer;

/**
 * A high-level configuration interface for setting up an application services.
 *
 * @see ConversionConfigurer
 */
public interface ServiceConfigurer extends EnvironmentConfigurer, ConversionConfigurer {

}
