package org.jmouse.core.mapping.runtime;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.reflection.InferredType;

import java.util.Objects;

public final class ObjectMapper implements Mapper {

    private final MappingContext context;

    public ObjectMapper(MappingContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public <T> T map(Object source, Class<T> target) {
        if (source == null) {
            return null;
        }

        Verify.nonNull(target, "target");

        InferredType   type = InferredType.forClass(target);
        MappingPlan<T> plan = context.planRegistry().planFor(source, type, context);

        return plan.execute(source, context);
    }
}
