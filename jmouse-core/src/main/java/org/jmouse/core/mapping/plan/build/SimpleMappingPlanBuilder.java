package org.jmouse.core.mapping.plan.build;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.plan.MappingPlan;

import java.util.Objects;

public final class SimpleMappingPlanBuilder implements MappingPlanBuilder {

    private final MapToBeanPlanBuilder mapToBeanPlanBuilder;
    private final MapToRecordPlanBuilder mapToRecordPlanBuilder;

    public SimpleMappingPlanBuilder(
            MapToBeanPlanBuilder mapToBeanPlanBuilder, MapToRecordPlanBuilder mapToRecordPlanBuilder
    ) {
        this.mapToBeanPlanBuilder = Verify.nonNull(mapToBeanPlanBuilder, "mapToBeanPlanBuilder");
        this.mapToRecordPlanBuilder = Verify.nonNull(mapToRecordPlanBuilder, "mapToRecordPlanBuilder");
    }

    @Override
    public <T> MappingPlan<T> build(Class<T> targetType) {
        Objects.requireNonNull(targetType, "targetType");
        if (targetType.isRecord()) {
            @SuppressWarnings("unchecked")
            MappingPlan<T> plan = (MappingPlan<T>) mapToRecordPlanBuilder.build((Class<? extends Record>) targetType);
            return plan;
        }
        return mapToBeanPlanBuilder.build(targetType);
    }
}
