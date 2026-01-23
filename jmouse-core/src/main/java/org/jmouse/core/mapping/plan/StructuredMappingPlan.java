package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.model.SourceModel;
import org.jmouse.core.mapping.model.SourceModelFactory;
import org.jmouse.core.mapping.model.TargetModel;
import org.jmouse.core.mapping.model.TargetSession;
import org.jmouse.core.mapping.runtime.MappingContext;

import java.util.List;
import java.util.Objects;

public final class StructuredMappingPlan implements MappingPlan<Object> {

    @FunctionalInterface
    public interface Step {
        void apply(SourceModel source, TargetSession session, MappingContext context);
    }

    private final TargetModel target;
    private final List<Step> steps;

    public StructuredMappingPlan(TargetModel target, List<Step> steps) {
        this.target = Objects.requireNonNull(target, "target");
        this.steps = Objects.requireNonNull(steps, "steps");
    }

    @Override
    public Object execute(Object source, MappingContext context) {
        SourceModel src = SourceModelFactory.defaultFactory(context).wrap(source);
        TargetSession session = target.newSession();

        for (Step step : steps) {
            step.apply(src, session, context);
        }

        return session.build();
    }
}
