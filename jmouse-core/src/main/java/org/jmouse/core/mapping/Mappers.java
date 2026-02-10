package org.jmouse.core.mapping;

import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.strategy.MappingStrategyContributor;
import org.jmouse.core.mapping.strategy.MappingStrategyRegistry;
import org.jmouse.core.mapping.strategy.array.ArrayStrategyContributor;
import org.jmouse.core.mapping.strategy.bean.JavaBeanStrategyContributor;
import org.jmouse.core.mapping.strategy.collection.ListStrategyContributor;
import org.jmouse.core.mapping.strategy.collection.SetStrategyContributor;
import org.jmouse.core.mapping.strategy.map.MapToMapStrategyContributor;
import org.jmouse.core.mapping.strategy.map.ObjectToMapStrategyContributor;
import org.jmouse.core.mapping.strategy.record.RecordStrategyContributor;
import org.jmouse.core.mapping.strategy.scalar.ScalarStrategyContributor;

import java.util.List;

/**
 * Entry point factory for creating {@link Mapper} instances and pre-configured {@link MapperBuilder}s. 🧰
 *
 * <p>This class centralizes default wiring for the mapping subsystem, including:</p>
 * <ul>
 *   <li>Default {@link MappingStrategyContributor} set (beans, records, scalars, maps, collections, arrays)</li>
 *   <li>Default {@link ObjectAccessorWrapper} for source/target access</li>
 *   <li>Default conversion service ({@link MapperConversion})</li>
 *   <li>Default policies via {@link MappingPolicy#defaults()}</li>
 *   <li>Default config via {@link MappingConfig}</li>
 *   <li>Default type mapping registry via {@link TypeMappingRegistry}</li>
 *   <li>Default plan registry via {@link MappingStrategyRegistry}</li>
 * </ul>
 *
 * <p>{@code Mappers} is a bootstrap utility; the returned {@link MapperBuilder} can be further customized
 * before building the final {@link Mapper}.</p>
 */
public class Mappers {

    /**
     * Default set of plan contributors used by {@link #builder()}.
     *
     * <p>Order matters: contributors are typically consulted in registration order when selecting
     * a suitable plan for a given mapping request.</p>
     */
    public static final List<MappingStrategyContributor> DEFAULT_CONTRIBUTORS = List.of(
            new JavaBeanStrategyContributor(),
            new RecordStrategyContributor(),
            new ScalarStrategyContributor(),
            new MapToMapStrategyContributor(),
            new ObjectToMapStrategyContributor(),
            new ListStrategyContributor(),
            new SetStrategyContributor(),
            new ArrayStrategyContributor()
    );

    /**
     * Create a mapper with the default configuration.
     *
     * @return default mapper instance
     */
    public static Mapper defaultMapper() {
        return builder().build();
    }

    /**
     * Create a {@link MapperBuilder} preconfigured with the framework defaults.
     *
     * <p>The builder is initialized with:</p>
     * <ul>
     *   <li>{@link ObjectAccessorWrapper}</li>
     *   <li>{@link MapperConversion}</li>
     *   <li>{@link MappingPolicy#defaults()}</li>
     *   <li>default {@link MappingConfig} instance</li>
     *   <li>empty {@link TypeMappingRegistry}</li>
     *   <li>{@link MappingStrategyRegistry} with {@link #DEFAULT_CONTRIBUTORS}</li>
     * </ul>
     *
     * @return preconfigured mapper builder
     */
    public static MapperBuilder builder() {
        return new MapperBuilder()
                .wrapper(new ObjectAccessorWrapper())
                .conversion(new MapperConversion())
                .policy(MappingPolicy.defaults())
                .config(MappingConfig.builder().build())
                .registry(TypeMappingRegistry.builder().build())
                .planRegistry(new MappingStrategyRegistry(DEFAULT_CONTRIBUTORS));
    }

}
