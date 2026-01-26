package org.jmouse.core.mapping.plan.cache;

import org.jmouse.core.mapping.plan.MappingPlan;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MappingPlanCache {

    private final ConcurrentHashMap<PlanKey, MappingPlan<?>> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> compute(PlanKey key, Supplier<MappingPlan<T>> supplier) {
        return (MappingPlan<T>) cache.computeIfAbsent(key, ignore -> supplier.get());
    }

}
