package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plan.MappingStrategy;
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
 *   <li>resolves an appropriate {@link MappingStrategy} from the configured plan registry</li>
 *   <li>executes the plan and returns the produced value</li>
 *   <li>handles {@link MappingException} by notifying plugins and rethrowing</li>
 * </ul>
 *
 * <p>When {@code source} is {@code null}, mapping returns {@code null} and no plan is executed.</p>
 *
 * @see MappingInvocation
 * @see MappingStrategy
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
     * Core mapping operation using {@link TypedValue}.
     *
     * <p>{@code typedValue} describes:</p>
     * <ul>
     *   <li>the target type metadata</li>
     *   <li>an optional target instance (existing or supplier-backed)</li>
     * </ul>
     *
     * @param source source object (may be {@code null})
     * @param typedValue target type descriptor and optional instance holder
     * @param <T> target type
     * @return mapped value (may be {@code null} when {@code source} is {@code null})
     */
    @Override
    public <T> T map(Object source, TypedValue<T> typedValue) {
        if (source == null) {
            return null;
        }

        Verify.nonNull(typedValue, "typedValue");

        InferredType      type          = typedValue.getType();
        MappingInvocation invocation    = MappingInvocation.begin(context, source, source.getClass(), type);
        MappingContext     scopedContext   = invocation.context();
        MappingStrategy<T> mappingStrategy = scopedContext.strategyRegistry().planFor(source, typedValue, scopedContext);

        try {
            T value = mappingStrategy.execute(source, scopedContext);
            return invocation.finish(source, value, type);
        } catch (MappingException mappingException) {
            if (!(invocation.fail(mappingException) instanceof MappingException exception)) {
                return null;
            }
            throw exception;
        }
    }
}
