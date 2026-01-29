package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.reflection.InferredType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default {@link Mapper} implementation that executes mapping plans using a {@link MappingContext}. 🧠
 *
 * <p>{@code ObjectMapper} is the main runtime entry point used by the mapping subsystem. It:</p>
 * <ul>
 *   <li>creates a {@link MappingInvocation} to manage root scope and plugin lifecycle</li>
 *   <li>resolves an appropriate {@link MappingPlan} from the configured plan registry</li>
 *   <li>executes the plan and returns the produced value</li>
 *   <li>handles {@link MappingException} by notifying plugins and rethrowing</li>
 * </ul>
 *
 * <p>When {@code source} is {@code null}, mapping returns {@code null} and no plan is executed.</p>
 *
 * @see MappingInvocation
 * @see MappingPlan
 * @see MappingContext
 */
public final class ObjectMapper implements Mapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectMapper.class);

    private final MappingContext context;

    /**
     * Create a mapper backed by the given {@link MappingContext}.
     *
     * @param context mapping context (never {@code null})
     * @throws NullPointerException if {@code context} is {@code null}
     */
    public ObjectMapper(MappingContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Map the given {@code source} into the requested inferred target {@code type}.
     *
     * <p>Plan resolution is delegated to the configured plan registry:
     * {@code planRegistry.planFor(source, type, context)}.</p>
     *
     * <p>Any {@link MappingException} thrown by the plan is logged and rethrown after
     * notifying the invocation bus via {@link MappingInvocation#fail(MappingException)}.</p>
     *
     * @param source source object (may be {@code null})
     * @param type inferred target type (never {@code null})
     * @param <T> target type
     * @return mapped value, or {@code null} when {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code type} is {@code null}
     * @throws MappingException when mapping fails
     */
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
