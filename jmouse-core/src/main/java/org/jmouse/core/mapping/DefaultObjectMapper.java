package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.model.SourceModel;
import org.jmouse.core.mapping.model.SourceModelFactory;
import org.jmouse.core.mapping.model.TargetModel;
import org.jmouse.core.mapping.model.TargetModelFactory;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.build.CompositeMappingPlanBuilder;
import org.jmouse.core.mapping.plan.build.PlanBuildContext;
import org.jmouse.core.mapping.plan.cache.MappingPlanCache;
import org.jmouse.core.mapping.plan.cache.PlanKey;
import org.jmouse.core.mapping.plan.contributor.StructuredToStructuredContributor;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.mapping.values.ValueKind;

import java.util.List;

public final class DefaultObjectMapper implements ObjectMapper {

    private final MappingConfig config;

    private final SourceModelFactory sourceFactory = new SourceModelFactory();
    private final TargetModelFactory targetFactory = new TargetModelFactory();

    private final MappingPlanCache            cache = new MappingPlanCache();
    private final CompositeMappingPlanBuilder builder;

    public DefaultObjectMapper(MappingConfig config) {
        this.config = Verify.nonNull(config, "config");
        this.builder = new CompositeMappingPlanBuilder(
                List.of(new StructuredToStructuredContributor()),
                new PlanBuildContext(this, config)
        );
    }

    @Override
    public <T> T map(Object source, Class<T> targetType) {
        Verify.nonNull(targetType, "targetType");

        MappingContext context = new MappingContext(this, config.policy(), config.conversion());

        SourceModel sourceModel = sourceFactory.wrap(source);
        TargetModel targetModel = targetFactory.forType(targetType);

        ValueKind sourceKind  = sourceModel.kind();
        int       fingerprint = config.policy().hashCode();

        MappingPlan<T> plan = cache.compute(
                new PlanKey(sourceKind, targetType, fingerprint),
                () -> builder.build(sourceModel, targetModel)
        );

        return plan.execute(source, context);
    }

    @Override
    public void map(Object source, Object target) {
        throw new UnsupportedOperationException("Merge mapping is out of scope for Stage 3.1");
    }
}
