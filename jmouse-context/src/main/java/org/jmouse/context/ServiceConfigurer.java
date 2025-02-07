package org.jmouse.context;

import org.jmouse.core.convert.ConversionConfigurer;
import org.jmouse.core.env.EnvironmentConfigurer;
import org.jmouse.common.mapping.MappingConfigurer;

/**
 * A high-level configuration interface for setting up an application services.
 *
 * @see MappingConfigurer
 * @see ConversionConfigurer
 */
public interface ServiceConfigurer extends EnvironmentConfigurer, MappingConfigurer, ConversionConfigurer {

}
