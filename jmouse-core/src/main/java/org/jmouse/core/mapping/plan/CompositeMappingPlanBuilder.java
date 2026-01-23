package org.jmouse.core.mapping.plan;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.model.SourceModel;
import org.jmouse.core.mapping.model.TargetModel;
import org.jmouse.core.mapping.plan.spi.MappingPlanContributor;
import org.jmouse.core.mapping.plan.spi.PlanBuildContext;

import java.util.List;

public final class CompositeMappingPlanBuilder {

    private final List<MappingPlanContributor> contributors;
    private final PlanBuildContext context;

    public CompositeMappingPlanBuilder(List<MappingPlanContributor> contributors, PlanBuildContext context) {
        this.contributors = Verify.nonNull(contributors, "contributors");
        this.context = Verify.nonNull(context, "context");
    }

    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> build(SourceModel source, TargetModel target) {
        for (MappingPlanContributor contributor : contributors) {
            if (contributor.supports(source, target)) {
                return (MappingPlan<T>) contributor.build(source, target, context);
            }
        }
        throw new IllegalStateException("No contributor for source=" + source.kind() + " target=" + target.kind());
    }
}
