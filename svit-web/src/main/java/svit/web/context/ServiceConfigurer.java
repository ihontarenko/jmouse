package svit.web.context;

import svit.convert.ConversionConfigurer;
import svit.mapping.MappingConfigurer;

/**
 * A high-level configuration interface for setting up an application services.
 *
 * @see MappingConfigurer
 * @see ConversionConfigurer
 */
public interface ServiceConfigurer extends MappingConfigurer, ConversionConfigurer {

}
