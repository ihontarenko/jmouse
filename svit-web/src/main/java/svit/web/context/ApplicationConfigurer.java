package svit.web.context;

import svit.beans.BeanContainer;
import svit.convert.ConversionConfigurer;
import svit.mapping.MappingConfigurer;

/**
 * A high-level configuration interface for setting up an application context. By extending
 * both {@link MappingConfigurer} and {@link ConversionConfigurer}, implementations can
 * configure mappings, conversions, and optionally register singletons within a
 * {@link BeanContainer}.
 *
 * @see MappingConfigurer
 * @see ConversionConfigurer
 * @see BeanContainer
 */
public interface ApplicationConfigurer extends MappingConfigurer, ConversionConfigurer {

    /**
     * Registers any required singletons with the given {@link BeanContainer}. The default
     * implementation is a no-op; overriding implementations can add custom logic for
     * instantiating and wiring up singletons that should be globally available in
     * the application context.
     *
     * @param container the {@link BeanContainer} in which to register singletons
     */
    default void registerSingleton(BeanContainer container) {
        // no-op
    }

}
