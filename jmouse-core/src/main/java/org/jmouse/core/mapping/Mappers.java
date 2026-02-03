package org.jmouse.core.mapping;

import org.jmouse.core.bind.StandardAccessorWrapper;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.plan.MappingStrategyRegistry;
import org.jmouse.core.mapping.plan.array.ArrayPlanContributor;
import org.jmouse.core.mapping.plan.bean.JavaBeanPlanContributor;
import org.jmouse.core.mapping.plan.collection.ListPlanContributor;
import org.jmouse.core.mapping.plan.collection.SetPlanContributor;
import org.jmouse.core.mapping.plan.map.MapToMapPlanContributor;
import org.jmouse.core.mapping.plan.map.ObjectToMapPlanContributor;
import org.jmouse.core.mapping.plan.record.RecordPlanContributor;
import org.jmouse.core.mapping.plan.scalar.ScalarPlanContributor;

import java.util.List;

/**
 * Entry point factory for creating {@link Mapper} instances and pre-configured {@link MapperBuilder}s. 🧰
 *
 * <p>This class centralizes default wiring for the mapping subsystem, including:</p>
 * <ul>
 *   <li>Default {@link MappingPlanContributor} set (beans, records, scalars, maps, collections, arrays)</li>
 *   <li>Default {@link StandardAccessorWrapper} for source/target access</li>
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
    public static final List<MappingPlanContributor> DEFAULT_CONTRIBUTORS = List.of(
            new JavaBeanPlanContributor(),
            new RecordPlanContributor(),
            new ScalarPlanContributor(),
            new MapToMapPlanContributor(),
            new ObjectToMapPlanContributor(),
            new ListPlanContributor(),
            new SetPlanContributor(),
            new ArrayPlanContributor()
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
     *   <li>{@link StandardAccessorWrapper}</li>
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
                .wrapper(new StandardAccessorWrapper())
                .conversion(new MapperConversion())
                .policy(MappingPolicy.defaults())
                .config(MappingConfig.builder().build())
                .registry(TypeMappingRegistry.builder().build())
                .planRegistry(new MappingStrategyRegistry(DEFAULT_CONTRIBUTORS));
    }

}
