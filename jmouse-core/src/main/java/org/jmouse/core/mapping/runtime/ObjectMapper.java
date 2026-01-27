package org.jmouse.core.mapping.runtime;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.reflection.InferredType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class ObjectMapper implements Mapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectMapper.class);

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

        try {
            InferredType   type = InferredType.forClass(target);
            MappingPlan<T> plan = context.planRegistry().planFor(source, type, context);

            return plan.execute(source, context);
        } catch (MappingException mappingException) {
            LOGGER.error("[{}]: {}", mappingException.code(), mappingException.getMessage());
            throw mappingException;
        }
    }
}
