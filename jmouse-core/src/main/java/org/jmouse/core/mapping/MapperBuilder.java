package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.plan.PlanRegistry;

/**
 * Fluent builder for constructing a {@link Mapper} with explicit wiring. 🧱
 *
 * <p>{@code MapperBuilder} collects all required mapping components and validates that the mapper
 * can be built in a consistent state.</p>
 *
 * <p>Required components:</p>
 * <ul>
 *   <li>{@link PlanRegistry} - plan selection/registry</li>
 *   <li>{@link ObjectAccessorWrapper} - source/target accessor wrapper</li>
 *   <li>{@link Conversion} - conversion service</li>
 *   <li>{@link TypeMappingRegistry} - type mapping specifications/overrides</li>
 *   <li>{@link MappingPolicy} - mapping behavior policy</li>
 *   <li>{@link MappingConfig} - runtime configuration knobs</li>
 * </ul>
 *
 * <p>The builder creates a {@link MappingContext} and returns the {@link Mapper} instance
 * produced by that context.</p>
 *
 * @see Mappers#builder()
 */
public final class MapperBuilder {

    private PlanRegistry          planRegistry;
    private ObjectAccessorWrapper wrapper;
    private Conversion            conversion;
    private TypeMappingRegistry   mappingRegistry;
    private MappingPolicy         policy;
    private MappingConfig         config;

    /**
     * Set the plan registry used to resolve mapping plans.
     *
     * @param planRegistry plan registry
     * @return this builder
     */
    public MapperBuilder planRegistry(PlanRegistry planRegistry) {
        this.planRegistry = planRegistry;
        return this;
    }

    /**
     * Set the accessor wrapper used to wrap source objects into accessors.
     *
     * @param wrapper accessor wrapper
     * @return this builder
     */
    public MapperBuilder wrapper(ObjectAccessorWrapper wrapper) {
        this.wrapper = wrapper;
        return this;
    }

    /**
     * Set the conversion service used for scalar and type conversions.
     *
     * @param conversion conversion service
     * @return this builder
     */
    public MapperBuilder conversion(Conversion conversion) {
        this.conversion = conversion;
        return this;
    }

    /**
     * Set the type mapping registry used to resolve explicit mapping specifications.
     *
     * @param mappingRegistry type mapping registry
     * @return this builder
     */
    public MapperBuilder registry(TypeMappingRegistry mappingRegistry) {
        this.mappingRegistry = mappingRegistry;
        return this;
    }

    /**
     * Set mapping policy controlling engine behavior.
     *
     * @param policy mapping policy
     * @return this builder
     */
    public MapperBuilder policy(MappingPolicy policy) {
        this.policy = policy;
        return this;
    }

    /**
     * Set mapping configuration (tunable runtime options).
     *
     * @param config mapping configuration
     * @return this builder
     */
    public MapperBuilder config(MappingConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Build a {@link Mapper} from the configured components.
     *
     * <p>This method validates that all required dependencies were provided and then constructs
     * a {@link MappingContext}. The returned mapper is obtained from {@link MappingContext#mapper()}.</p>
     *
     * @return configured mapper instance
     * @throws NullPointerException if any required component is missing
     */
    public Mapper build() {

        Verify.nonNull(planRegistry, "planRegistry");
        Verify.nonNull(wrapper, "wrapper");
        Verify.nonNull(conversion, "conversion");
        Verify.nonNull(mappingRegistry, "mappingRegistry");
        Verify.nonNull(policy, "policy");
        Verify.nonNull(config, "config");

        MappingContext context = new MappingContext(
                ObjectMapper::new,
                planRegistry,
                wrapper,
                conversion,
                mappingRegistry,
                policy,
                config,
                MappingScope.root(null)
        );

        return context.mapper();
    }
}
