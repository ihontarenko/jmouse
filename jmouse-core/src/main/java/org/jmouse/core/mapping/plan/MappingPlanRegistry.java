package org.jmouse.core.mapping.plan;

import org.jmouse.core.Sorter;
import org.jmouse.core.Verify;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MappingPlanRegistry implements PlanRegistry {

    private final List<MappingPlanContributor> contributors;
    private final Map<PlanKey, MappingPlan<?>> cache = new ConcurrentHashMap<>();

    public MappingPlanRegistry(List<MappingPlanContributor> contributors) {
        List<MappingPlanContributor> sorted = new ArrayList<>(contributors);
        Sorter.sort(sorted);
        this.contributors = List.copyOf(Verify.nonNull(sorted, "contributors"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> planFor(Object source, InferredType targetType, MappingContext context) {
        PlanKey key = new PlanKey(source.getClass(), targetType.getRawType());
        return (MappingPlan<T>) cache.computeIfAbsent(key, ignore -> build(source, targetType, context));
    }

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

    private record PlanKey(Class<?> sourceType, Class<?> targetType) {}
}
