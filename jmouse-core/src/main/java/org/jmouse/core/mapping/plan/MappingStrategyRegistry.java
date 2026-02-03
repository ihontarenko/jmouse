package org.jmouse.core.mapping.plan;

import org.jmouse.core.Sorter;
import org.jmouse.core.Verify;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link StrategyRegistry} implementation that resolves and caches {@link MappingStrategy}s. 🧠
 *
 * <p>{@code MappingStrategyRegistry} maintains an ordered list of {@link MappingPlanContributor}s and
 * selects the first contributor that {@linkplain MappingPlanContributor#supports(Object, InferredType, MappingContext)
 * supports} a given mapping request.</p>
 *
 * <h3>Contributor ordering</h3>
 * <p>Contributors are sorted during construction using {@link Sorter#sort(List)}. This allows contributors
 * to define priority (e.g., via a comparable contract or an ordering annotation supported by your sorter).</p>
 *
 * <h3>Caching</h3>
 * <p>Resolved plans are cached in a thread-safe map keyed by {@code (sourceClass, targetRawClass)}.</p>
 *
 * <p><strong>Note:</strong> the cache key uses {@link InferredType#getRawType()} and does not include
 * generic parameters. If generic-sensitive planning is required, extend {@code PlanKey} accordingly.</p>
 *
 * @see MappingPlanContributor
 * @see MappingStrategy
 */
public final class MappingStrategyRegistry implements StrategyRegistry {

    private final List<MappingPlanContributor>     contributors;
    private final Map<PlanKey, MappingStrategy<?>> cache = new ConcurrentHashMap<>();

    /**
     * Create a plan registry with the provided contributors.
     *
     * <p>The contributors are copied and sorted to produce a stable resolution order.</p>
     *
     * @param contributors contributor list
     * @throws IllegalArgumentException if {@code contributors} is {@code null}
     */
    public MappingStrategyRegistry(List<MappingPlanContributor> contributors) {
        List<MappingPlanContributor> sorted = new ArrayList<>(contributors);
        Sorter.sort(sorted);
        this.contributors = List.copyOf(Verify.nonNull(sorted, "contributors"));
    }

    /**
     * Resolve a {@link MappingStrategy} for the given mapping request.
     *
     * <p>The plan is cached using a key composed of:</p>
     * <ul>
     *   <li>the runtime {@code source.getClass()}</li>
     *   <li>the raw target class from {@code typedValue.getType().getRawType()}</li>
     * </ul>
     *
     * <p><strong>Note:</strong> the cache key does not include generic parameters or the target instance
     * provided by {@link TypedValue}. If instance-sensitive planning is required, extend {@code PlanKey}
     * to include the relevant aspects.</p>
     *
     * @param source source object (must be non-null)
     * @param typedValue target typed value (type metadata + optional instance holder)
     * @param context mapping context
     * @param <T> plan output type
     * @return cached or newly built mapping plan
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> planFor(Object source, TypedValue<T> typedValue, MappingContext context) {
        InferredType type = typedValue.getType();
        PlanKey      key  = new PlanKey(source.hashCode(), type.hashCode());
//        return (MappingStrategy<T>) cache.computeIfAbsent(key, ignore -> build(source, typedValue, context));
        return (MappingStrategy<T>) build(source, typedValue, context);
    }

    /**
     * Build a {@link MappingStrategy} by scanning registered contributors and selecting the first match.
     *
     * <p>Contributors are consulted in registry order. The first contributor that returns {@code true}
     * from {@link MappingPlanContributor#supports(Object, InferredType, MappingContext)} is responsible
     * for building the plan via {@link MappingPlanContributor#build(TypedValue, MappingContext)}.</p>
     *
     * @param source source object
     * @param typedValue typed target descriptor (type metadata + optional instance holder)
     * @param context mapping context
     * @return built plan
     * @throws MappingException if no contributor supports the mapping request
     */
    private MappingStrategy<?> build(Object source, TypedValue<?> typedValue, MappingContext context) {
        InferredType targetType = typedValue.getType();
        InferredType sourceType = InferredType.forInstance(source);

        for (MappingPlanContributor contributor : contributors) {
            if (contributor.supports(source, targetType, context)) {
                return contributor.build(typedValue, context);
            }
        }

        throw new MappingException(
                "no_plan_contributor",
                "No plan contributor for source='%s', target='%s'".formatted(sourceType.getClassType(), targetType),
                null
        ).withMeta("source", sourceType).withMeta("target", targetType);
    }

    /**
     * Cache key for mapping plans.
     *
     * @param sourceHashCode hash code source
     * @param targetHashCode hash code target
     */
    private record PlanKey(Integer sourceHashCode, Integer targetHashCode) {}
}
