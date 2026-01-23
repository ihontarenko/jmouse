package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.model.SourceModelFactory;
import org.jmouse.core.mapping.model.TargetModelFactory;
import org.jmouse.core.mapping.plan.CompositeMappingPlanBuilder;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.cache.MappingPlanCache;
import org.jmouse.core.mapping.plan.cache.PlanKey;
import org.jmouse.core.mapping.plan.contrib.StructuredToStructuredContributor;
import org.jmouse.core.mapping.plan.spi.PlanBuildContext;
import org.jmouse.core.mapping.runtime.MappingContext;

import java.util.List;

public final class DefaultObjectMapper implements ObjectMapper {

    private final MappingConfig               config;
    private final TargetModelFactory          factory = new TargetModelFactory();
    private final MappingPlanCache            cache   = new MappingPlanCache();
    private final CompositeMappingPlanBuilder builder;

    public DefaultObjectMapper(MappingConfig config) {
        this.config = Verify.nonNull(config, "config");
        this.builder = new CompositeMappingPlanBuilder(
                List.of(new StructuredToStructuredContributor()),
                new PlanBuildContext(this)
        );
    }

    @Override
    public <T> T map(Object source, Class<T> targetType) {
        Verify.nonNull(targetType, "targetType");

        MappingContext context = new MappingContext(this, config.policy(), config.conversion());

        var sourceModel = SourceModelFactory.defaultFactory(context).wrap(source);
        var targetModel = factory.forType(targetType);

        PlanKey key = new PlanKey(sourceModel.kind(), targetType, config.policy().hashCode());

        MappingPlan<T> plan = cache.compute(key, () -> builder.build(sourceModel, targetModel));

        return plan.execute(source, context);
    }

    @Override
    public void map(Object source, Object target) {
        throw new UnsupportedOperationException("Merge mapping comes later");
    }
}
