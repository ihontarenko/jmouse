package org.jmouse.mapper;

import org.jmouse.core.Customizer;
import org.jmouse.core.SingletonSupplier;
import org.jmouse.core.Verify;
import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.mapper.binding.TypeMappingBuilder;
import org.jmouse.mapper.binding.TypeMappingRegistry;
import org.jmouse.mapper.binding.annotation.AnnotationRuleSource;
import org.jmouse.mapper.config.MappingConfig;
import org.jmouse.mapper.config.MappingPolicy;
import org.jmouse.mapper.strategy.MappingStrategyContributor;
import org.jmouse.mapper.strategy.MappingStrategyRegistry;
import org.jmouse.mapper.strategy.array.ArrayStrategyContributor;
import org.jmouse.mapper.strategy.bean.JavaBeanStrategyContributor;
import org.jmouse.mapper.strategy.converted.ConvertedPairStrategyContributor;
import org.jmouse.mapper.strategy.collection.ListStrategyContributor;
import org.jmouse.mapper.strategy.collection.SetStrategyContributor;
import org.jmouse.mapper.strategy.map.MapToMapStrategyContributor;
import org.jmouse.mapper.strategy.map.ObjectToMapStrategyContributor;
import org.jmouse.mapper.strategy.record.RecordStrategyContributor;
import org.jmouse.mapper.strategy.scalar.ScalarStrategyContributor;

import java.util.List;
import java.util.function.Supplier;

/**
 * Entry point for obtaining a {@link Mapper}. 🧰
 *
 * <h2>Pouring A into B</h2>
 * <p>Properties that share a name need no configuration at all:</p>
 * <pre>{@code
 * UserB target = Mappers.defaultMapper().map(source, UserB.class);
 * UserA merged = Mappers.defaultMapper().map(source, existingUserA);   // into an existing instance
 * }</pre>
 *
 * <h2>When two names disagree</h2>
 * <p>State the binding inline; everything else stays defaulted:</p>
 * <pre>{@code
 * Mapper mapper = Mappers.mapper(UserA.class, UserB.class, user -> user
 *         .rename("dateOfBirth", "birthDay"));
 * }</pre>
 *
 * <p>For several type pairs at once:</p>
 * <pre>{@code
 * Mapper mapper = Mappers.mapper(rules -> rules
 *         .mapping(UserA.class, UserB.class, user -> user.rename("dateOfBirth", "birthDay"))
 *         .mapping(UserB.class, UserA.class, user -> user.rename("birthDay", "dateOfBirth")));
 * }</pre>
 *
 * <h2>When more than rules must change</h2>
 * <p>{@link #builder()} exposes the whole wiring - policies, config, conversion, strategies - and
 * {@link MapperBuilder#rules(Customizer)} still adds bindings without discarding the defaults.</p>
 *
 * <p>Defaults installed here: the {@link #DEFAULT_CONTRIBUTORS} strategy set, an
 * {@link ObjectAccessorWrapper}, {@link MapperConversion}, {@link MappingPolicy#defaults()},
 * {@link MappingConfig} defaults, and {@link AnnotationRuleSource} so mapping annotations are read
 * with no wiring.</p>
 */
public class Mappers {

    private static final Supplier<Mapper> SINGLETON = SingletonSupplier.of(() -> builder().build());

    /**
     * Default set of strategy contributors used by {@link #builder()}.
     *
     * <p>⚠️ Resolution order is decided by each contributor's {@code @Priority}, not by this list -
     * {@link MappingStrategyRegistry} sorts whatever it is given. This list says who takes part.</p>
     */
    public static final List<MappingStrategyContributor> DEFAULT_CONTRIBUTORS = List.of(
            // ⚠️ First in the list AND first by priority, and it has to be both: a pair converted whole
            // has a target that is a bean, a record or a scalar like any other, so every contributor
            // below would claim it and map it property by property. It matches only where a rule
            // actually declares a whole-target expression, which is a very small set of pairs.
            new ConvertedPairStrategyContributor(),
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
     * The shared mapper with the default configuration and no explicit bindings.
     *
     * <p>Same-named properties, annotations, conversions and deep object graphs all work through
     * this one. It is a singleton, so it is safe to call on every mapping.</p>
     *
     * @return default mapper instance
     */
    public static Mapper defaultMapper() {
        return SINGLETON.get();
    }

    /**
     * A default mapper carrying the given bindings.
     *
     * <p>Everything {@link #defaultMapper()} does still applies; the rules are added, not
     * substituted.</p>
     *
     * <pre>{@code
     * Mapper mapper = Mappers.mapper(rules -> rules
     *         .mapping(UserA.class, UserB.class, user -> user.rename("dateOfBirth", "birthDay")));
     * }</pre>
     *
     * @param customizer receives the rule registry builder
     * @return mapper with the framework defaults plus the given rules
     */
    public static Mapper mapper(Customizer<TypeMappingRegistry.Builder> customizer) {
        return builder().rules(Verify.nonNull(customizer, "customizer")).build();
    }

    /**
     * A default mapper carrying bindings for one type pair.
     *
     * <p>The shortest form of the common case: two types that agree on most property names and
     * disagree on a few.</p>
     *
     * <pre>{@code
     * Mapper mapper = Mappers.mapper(UserA.class, UserB.class, user -> user
     *         .rename("dateOfBirth", "birthDay")
     *         .constant("role", "USER"));
     * }</pre>
     *
     * @param sourceType source type the bindings apply to
     * @param targetType target type the bindings apply to
     * @param customizer receives the type mapping builder for the pair
     * @param <S> source type
     * @param <T> target type
     * @return mapper with the framework defaults plus the given rules
     */
    public static <S, T> Mapper mapper(
            Class<S> sourceType,
            Class<T> targetType,
            Customizer<TypeMappingBuilder<S, T>> customizer
    ) {
        return mapper(rules -> rules.mapping(sourceType, targetType, customizer));
    }

    /**
     * A {@link MapperBuilder} preconfigured with the framework defaults.
     *
     * <p>Reach for this when something other than the rules has to change. To add rules, prefer
     * {@link MapperBuilder#rules(Customizer)} over
     * {@link MapperBuilder#mappingRegistry(TypeMappingRegistry)} - the latter replaces the registry
     * and takes {@link AnnotationRuleSource} with it.</p>
     *
     * @return preconfigured mapper builder
     */
    public static MapperBuilder builder() {
        return new MapperBuilder()
                .wrapper(new ObjectAccessorWrapper())
                .conversion(new MapperConversion())
                .policy(MappingPolicy.defaults())
                .config(MappingConfig.builder().build())
                .rules(rules -> rules.ruleSource(new AnnotationRuleSource()))
                .strategyRegistry(new MappingStrategyRegistry(DEFAULT_CONTRIBUTORS));
    }

}
