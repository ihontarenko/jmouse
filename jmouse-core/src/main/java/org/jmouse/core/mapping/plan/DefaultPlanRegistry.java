package org.jmouse.core.mapping.plan;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultPlanRegistry implements PlanRegistry {

    private final List<MappingPlanContributor> contributors;
    private final Map<PlanKey, MappingPlan<?>> cache = new ConcurrentHashMap<>();

    public DefaultPlanRegistry(List<MappingPlanContributor> contributors) {
        this.contributors = List.copyOf(Verify.nonNull(contributors, "contributors"));
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
