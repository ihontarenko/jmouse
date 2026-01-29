package org.jmouse.core.mapping.plan;

import org.jmouse.core.Sorter;
import org.jmouse.core.Verify;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link PlanRegistry} implementation that resolves and caches {@link MappingPlan}s. 🧠
 *
 * <p>{@code MappingPlanRegistry} maintains an ordered list of {@link MappingPlanContributor}s and
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
 * @see MappingPlan
 */
public final class MappingPlanRegistry implements PlanRegistry {

    private final List<MappingPlanContributor> contributors;
    private final Map<PlanKey, MappingPlan<?>> cache = new ConcurrentHashMap<>();

    /**
     * Create a plan registry with the provided contributors.
     *
     * <p>The contributors are copied and sorted to produce a stable resolution order.</p>
     *
     * @param contributors contributor list
     * @throws IllegalArgumentException if {@code contributors} is {@code null}
     */
    public MappingPlanRegistry(List<MappingPlanContributor> contributors) {
        List<MappingPlanContributor> sorted = new ArrayList<>(contributors);
        Sorter.sort(sorted);
        this.contributors = List.copyOf(Verify.nonNull(sorted, "contributors"));
    }

    /**
     * Resolve a {@link MappingPlan} for the given mapping request.
     *
     * <p>Plans are memoized by {@code (sourceClass, targetRawClass)}.</p>
     *
     * @param source source object (must be non-null)
     * @param targetType inferred target type
     * @param context mapping context
     * @param <T> target type
     * @return mapping plan suitable for the request
     * @throws MappingException if no contributor can build a plan for the request
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> planFor(Object source, InferredType targetType, MappingContext context) {
        PlanKey key = new PlanKey(source.getClass(), targetType.getRawType());
        return (MappingPlan<T>) cache.computeIfAbsent(key, ignore -> build(source, targetType, context));
    }

    /**
     * Build a plan by scanning contributors and selecting the first compatible one.
     *
     * @param source source object
     * @param targetType inferred target type
     * @param context mapping context
     * @return built plan
     * @throws MappingException if no contributor supports the request
     */
    private MappingPlan<?> build(Object source, InferredType targetType, MappingContext context) {
        for (MappingPlanContributor contributor : contributors) {
            if (contributor.supports(source, targetType, context)) {
                return contributor.build(targetType, context);
            }
        }

        throw new MappingException(
                "no_plan_contributor",
                "No plan contributor for source='%s', target='%s'".formatted(source.getClass().getName(), targetType),
                null
        );
    }

    /**
     * Cache key for mapping plans.
     *
     * @param sourceType source runtime class
     * @param targetType target raw class
     */
    private record PlanKey(Class<?> sourceType, Class<?> targetType) {}
}
