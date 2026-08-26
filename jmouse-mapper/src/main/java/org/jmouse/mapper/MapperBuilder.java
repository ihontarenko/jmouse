package org.jmouse.mapper;

import org.jmouse.core.Customizer;
import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.convert.Conversion;
import org.jmouse.mapper.binding.TypeMappingRegistry;
import org.jmouse.mapper.config.MappingConfig;
import org.jmouse.mapper.config.MappingPolicy;
import org.jmouse.mapper.strategy.StrategyRegistry;

/**
 * Fluent builder for constructing a {@link Mapper} with explicit wiring. 🧱
 *
 * <p>{@code MapperBuilder} collects all required mapping components and validates that the mapper
 * can be built in a consistent state.</p>
 *
 * <p>Required components:</p>
 * <ul>
 *   <li>{@link StrategyRegistry} - strategy selection/registry</li>
 *   <li>{@link AccessorWrapper} - source/target accessor wrapper</li>
 *   <li>{@link Conversion} - conversion service</li>
 *   <li>{@link MappingPolicy} - mapping behavior policy</li>
 *   <li>{@link MappingConfig} - runtime configuration knobs</li>
 * </ul>
 *
 * <p>Mapping rules come from {@link #rules(Customizer)}, which adds to whatever the builder was
 * seeded with, or from {@link #mappingRegistry(TypeMappingRegistry)}, which replaces it outright.</p>
 *
 * <p>The builder creates a {@link MappingContext} and returns the {@link Mapper} instance
 * produced by that context.</p>
 *
 * @see Mappers#builder()
 */
public final class MapperBuilder {

    private final TypeMappingRegistry.Builder rules = TypeMappingRegistry.builder();

    private StrategyRegistry    strategyRegistry;
    private AccessorWrapper     wrapper;
    private Conversion          conversion;
    private TypeMappingRegistry mappingRegistry;
    private MappingPolicy       policy;
    private MappingConfig       config;

    /**
     * Set the strategy registry used to resolve mapping strategies.
     *
     * @param strategyRegistry strategy registry
     * @return this builder
     */
    public MapperBuilder strategyRegistry(StrategyRegistry strategyRegistry) {
        this.strategyRegistry = strategyRegistry;
        return this;
    }

    /**
     * Set the accessor wrapper used to wrap source objects into accessors.
     *
     * @param wrapper accessor wrapper
     * @return this builder
     */
    public MapperBuilder wrapper(AccessorWrapper wrapper) {
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
     * Add mapping rules on top of whatever this builder already carries.
     *
     * <p>This is the ordinary way to declare bindings, because it <em>adds</em>: the rule sources
     * {@link Mappers#builder()} installs - annotation support among them - are still there
     * afterwards.</p>
     *
     * <pre>{@code
     * Mapper mapper = Mappers.builder()
     *         .rules(rules -> rules.mapping(UserA.class, UserB.class, user -> user
     *                 .rename("dateOfBirth", "birthDay")))
     *         .build();
     * }</pre>
     *
     * @param customizer receives the rule registry builder
     * @return this builder
     */
    public MapperBuilder rules(Customizer<TypeMappingRegistry.Builder> customizer) {
        Verify.nonNull(customizer, "customizer").customize(rules);
        return this;
    }

    /**
     * Replace the rule registry outright with a prebuilt one.
     *
     * <p>⚠️ This <em>discards</em> everything {@link #rules(Customizer)} collected and every rule
     * source the builder was seeded with. A registry built by hand carries only the sources it was
     * given, so a mapper wired this way has no annotation support unless it registers
     * {@code AnnotationRuleSource} itself. Prefer {@link #rules(Customizer)} unless full control is
     * the point.</p>
     *
     * @param mappingRegistry type mapping registry
     * @return this builder
     */
    public MapperBuilder mappingRegistry(TypeMappingRegistry mappingRegistry) {
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
     * @throws IllegalArgumentException if any required component is missing
     */
    public Mapper build() {

        Verify.nonNull(strategyRegistry, "strategyRegistry");
        Verify.nonNull(wrapper, "wrapper");
        Verify.nonNull(conversion, "conversion");
        Verify.nonNull(policy, "policy");
        Verify.nonNull(config, "config");

        MappingContext context = new MappingContext(
                ObjectMapper::new,
                strategyRegistry,
                wrapper,
                conversion,
                mappingRegistry == null ? rules.build() : mappingRegistry,
                policy,
                config,
                MappingScope.root(null)
        );

        return context.mapper();
    }
}
