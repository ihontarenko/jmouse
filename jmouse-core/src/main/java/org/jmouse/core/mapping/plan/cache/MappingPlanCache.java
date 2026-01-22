package org.jmouse.core.mapping.plan.cache;

import org.jmouse.core.mapping.plan.MappingPlan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MappingPlanCache {

    private final Map<Class<?>, MappingPlan<?>> byTargetType = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> getOrCompute(Class<T> targetType, java.util.function.Supplier<MappingPlan<T>> supplier) {
        return (MappingPlan<T>) byTargetType.computeIfAbsent(targetType, k -> supplier.get());
    }
}
