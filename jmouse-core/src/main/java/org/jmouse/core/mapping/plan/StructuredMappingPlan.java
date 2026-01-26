package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.model.SourceModel;
import org.jmouse.core.mapping.model.SourceModelFactory;
import org.jmouse.core.mapping.model.TargetModel;
import org.jmouse.core.mapping.model.TargetSession;
import org.jmouse.core.mapping.runtime.MappingContext;

import java.util.List;
import java.util.Objects;

public final class StructuredMappingPlan implements MappingPlan<Object> {

    private final TargetModel        targetModel;
    private final List<Step>         steps;
    private final SourceModelFactory sourceModelFactory;

    @FunctionalInterface
    public interface Step {
        void apply(SourceModel source, TargetSession target, MappingContext context);
    }

    public StructuredMappingPlan(TargetModel targetModel, List<Step> steps, SourceModelFactory sourceModelFactory) {
        this.targetModel = Objects.requireNonNull(targetModel, "targetModel");
        this.steps = Objects.requireNonNull(steps, "steps");
        this.sourceModelFactory = Objects.requireNonNull(sourceModelFactory, "sourceModelFactory");
    }

    @Override
    public Object execute(Object source, MappingContext context) {
        SourceModel   sourceModel = sourceModelFactory.wrap(source);
        TargetSession session     = targetModel.newSession();

        for (Step step : steps) {
            step.apply(sourceModel, session, context);
        }

        return session.build();
    }
}
