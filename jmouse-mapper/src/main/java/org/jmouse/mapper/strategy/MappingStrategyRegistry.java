package org.jmouse.mapper.strategy;

import org.jmouse.core.Sorter;
import org.jmouse.core.Verify;
import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.mapper.errors.MappingException;
import org.jmouse.core.reflection.InferredType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link StrategyRegistry} implementation that resolves and caches {@link MappingStrategy}s. 🧠
 *
 * <p>{@code MappingStrategyRegistry} maintains an ordered list of {@link MappingStrategyContributor}s and
 * selects the first contributor that {@linkplain MappingStrategyContributor#supports(Object, InferredType, MappingContext)
 * supports} a given mapping request.</p>
 *
 * <h3>Contributor ordering</h3>
 * <p>Contributors are sorted using {@link Sorter#sort(List)}, which honours their {@code @Priority}.
 * The list passed to the constructor therefore documents membership, not resolution order.</p>
 *
 * <h3>Caching</h3>
 * <p>Resolution is memoized per {@code (sourceClass, targetType)}. Both {@code supports} and
 * {@code build} of every shipped contributor depend on exactly that pair - never on the source
 * instance or on a target instance carried by {@link TypedValue} - so one strategy per pair is
 * reusable across invocations. Registering a contributor drops the cache, since it may outrank a
 * contributor that already answered for some pair.</p>
 *
 * <p>⚠️ The table is nested - source class, then target type - rather than keyed on a pair, and it is
 * read before it is written to. This is asked <em>once per property of every mapped object</em>, from
 * a table that has been fully populated since the first object went through, and the obvious single
 * map answered each of those hits only after allocating twice: a key to look up with, and the
 * capturing lambda {@link java.util.concurrent.ConcurrentHashMap#computeIfAbsent} takes, which is
 * built whether or not it is needed. Nesting makes the first level an identity comparison on a class
 * and leaves the hit path with nothing to allocate at all; only a miss builds anything.</p>
 *
 * <p><strong>Contract for custom contributors:</strong> a contributor whose decision depends on the
 * source <em>instance</em> rather than its class breaks this memoization and must not be registered
 * here.</p>
 *
 * @see MappingStrategyContributor
 * @see MappingStrategy
 */
public final class MappingStrategyRegistry implements StrategyRegistry {

    private final List<MappingStrategyContributor> contributors;

    private final Map<Class<?>, Map<InferredType, MappingStrategy<?>>> strategies = new ConcurrentHashMap<>();

    /**
     * Create a strategy registry with the provided contributors.
     *
     * @param contributors contributor list
     * @throws IllegalArgumentException if {@code contributors} is {@code null}
     */
    public MappingStrategyRegistry(List<MappingStrategyContributor> contributors) {
        this.contributors = new ArrayList<>(Verify.nonNull(contributors, "contributors"));
        Sorter.sort(this.contributors);
    }

    /**
     * Register an additional contributor and drop the memoized strategies.
     *
     * @param contributor contributor to add
     * @return this registry
     */
    public MappingStrategyRegistry register(MappingStrategyContributor contributor) {
        contributors.add(Verify.nonNull(contributor, "contributor"));
        Sorter.sort(contributors);
        strategies.clear();
        return this;
    }

    /**
     * Resolve a {@link MappingStrategy} for the given mapping request.
     *
     * <p>The strategy is memoized per {@code (source.getClass(), typedValue.getType())}. The target
     * type is part of the key in full, generic arguments included, so {@code List<String>} and
     * {@code List<Integer>} never share an entry.</p>
     *
     * @param source source object (must be non-null)
     * @param typedValue target typed value (type metadata + optional instance holder)
     * @param context mapping context
     * @param <T> strategy output type
     * @return cached or newly built mapping strategy
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> strategyFor(Object source, TypedValue<T> typedValue, MappingContext context) {
        Class<?>     sourceType = source == null ? NullSource.class : source.getClass();
        InferredType targetType = typedValue.getType();

        Map<InferredType, MappingStrategy<?>> byTargetType = strategies.get(sourceType);
        MappingStrategy<?>                    strategy     = byTargetType == null
                ? null : byTargetType.get(targetType);

        if (strategy == null) {
            strategy = strategies
                    .computeIfAbsent(sourceType, ignored -> new ConcurrentHashMap<>())
                    .computeIfAbsent(targetType, ignored -> build(source, typedValue, context));
        }

        return (MappingStrategy<T>) strategy;
    }

    /**
     * Build a {@link MappingStrategy} by scanning registered contributors and selecting the first match.
     *
     * @param source source object
     * @param typedValue typed target descriptor (type metadata + optional instance holder)
     * @param context mapping context
     * @return built strategy
     * @throws MappingException if no contributor supports the mapping request
     */
    private MappingStrategy<?> build(Object source, TypedValue<?> typedValue, MappingContext context) {
        InferredType targetType = typedValue.getType();

        for (MappingStrategyContributor contributor : contributors) {
            if (contributor.supports(source, targetType, context)) {
                return contributor.build(source, typedValue, context);
            }
        }

        InferredType sourceType = InferredType.forInstance(source);

        throw new MappingException(
                ErrorCodes.STRATEGY_NO_CONTRIBUTOR,
                "No strategy contributor for source='%s', target='%s'".formatted(sourceType, targetType),
                null
        ).withMeta("source", sourceType).withMeta("target", targetType);
    }

    /**
     * Stands in for the class of a source that is {@code null}.
     *
     * <p>A null source is a legitimate request with an answer of its own, and it used to be keyed under
     * a {@code null} class. A {@link ConcurrentHashMap} accepts no null key, so it needs a class that
     * exists - and one nothing can ever be an instance of, so it can never collide with a real source.
     * This one is private and has no accessible constructor.</p>
     */
    private static final class NullSource {

        private NullSource() {
        }
    }
}
