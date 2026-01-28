package org.jmouse.core.mapping;

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
    public <T> T map(Object source, InferredType type) {
        if (source == null) {
            return null;
        }

        Verify.nonNull(type, "type");

        MappingInvocation invocation    = MappingInvocation.begin(context, source, source.getClass(), type);
        MappingContext    scopedContext = invocation.context();
        MappingPlan<T>    plan          = scopedContext.planRegistry().planFor(source, type, scopedContext);

        try {
            T value = plan.execute(source, scopedContext);
            return invocation.finish(source, value, type);
        } catch (MappingException mappingException) {
            LOGGER.error("[{}]: {}", mappingException.code(), mappingException.getMessage());
            throw invocation.fail(mappingException);
        }
    }
}
