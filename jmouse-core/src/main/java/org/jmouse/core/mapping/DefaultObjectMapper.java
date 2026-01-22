package org.jmouse.core.mapping;

import org.jmouse.core.mapping.config.ErrorHandlingPolicy;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.diagnostics.MappingDiagnostics;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.build.MapToBeanPlanBuilder;
import org.jmouse.core.mapping.plan.build.MapToRecordPlanBuilder;
import org.jmouse.core.mapping.plan.build.SimpleMappingPlanBuilder;
import org.jmouse.core.mapping.plan.cache.MappingPlanCache;
import org.jmouse.core.mapping.runtime.MappingContext;

import java.util.Map;
import java.util.Objects;

public final class DefaultObjectMapper implements ObjectMapper {

    private final MappingConfig config;

    private final MappingPlanCache         planCache;
    private final SimpleMappingPlanBuilder planBuilder;

    public DefaultObjectMapper(MappingConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.planCache = new MappingPlanCache();
        this.planBuilder = new SimpleMappingPlanBuilder(
                new MapToBeanPlanBuilder(),
                new MapToRecordPlanBuilder()
        );
    }

    @Override
    public <T> T map(Object source, Class<T> targetType) {
        Objects.requireNonNull(targetType, "targetType");

        if (source != null && !(source instanceof Map<?, ?>)) {
            throw new UnsupportedOperationException("Stage 3 supports Map source only");
        }

        MappingDiagnostics diagnostics = new MappingDiagnostics();
        MappingContext context = new MappingContext(
                this,
                config.policy(),
                config.conversion(),
                config.virtualPropertyResolver(),
                diagnostics
        );

        MappingPlan<T> plan = planCache.getOrCompute(targetType, () -> planBuilder.build(targetType));
        T result = plan.execute(source, context);

        // In fail-fast mode, exceptions already thrown. For collect-and-throw, respect diagnostics.
        if (config.policy().errorHandlingPolicy() == ErrorHandlingPolicy.COLLECT_AND_THROW
                && diagnostics.hasErrors()) {
            throw new MappingException("Mapping failed", diagnostics.problems());
        }

        return result;
    }

    @Override
    public void map(Object source, Object target) {
        throw new UnsupportedOperationException("Merge mapping is not implemented in Stage 3");
    }
}
