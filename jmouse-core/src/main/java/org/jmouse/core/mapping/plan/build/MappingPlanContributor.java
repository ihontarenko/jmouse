package org.jmouse.core.mapping.plan.build;

import org.jmouse.core.mapping.model.SourceModel;
import org.jmouse.core.mapping.model.TargetModel;
import org.jmouse.core.mapping.plan.MappingPlan;

public interface MappingPlanContributor {

    boolean supports(SourceModel source, TargetModel target);

    MappingPlan<?> build(SourceModel source, TargetModel target, PlanBuildContext context);
}
