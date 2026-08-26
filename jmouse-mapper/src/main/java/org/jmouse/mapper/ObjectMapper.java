package org.jmouse.mapper;

import org.jmouse.core.Verify;
import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.config.MappingConfig;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.mapper.errors.MappingException;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.core.reflection.InferredType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default {@link Mapper} implementation that executes mapping plans using a {@link MappingContext}. 🧠
 *
 * <p>{@code ObjectMapper} is the main runtime entry point used by the mapping subsystem. It:</p>
 * <ul>
 *   <li>creates a {@link MappingInvocation} to manage sourceRoot scope and plugin lifecycle</li>
 *   <li>resolves an appropriate {@link MappingStrategy} from the configured plan registry</li>
 *   <li>executes the plan and returns the produced value</li>
 *   <li>hands a {@link MappingException} to {@link MappingInvocation#recover(MappingException)},
 *       which notifies plugins and either rethrows or swallows it per the configured error policy</li>
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

        InferredType       type          = typedValue.getType();
        MappingInvocation  invocation    = MappingInvocation.begin(context, source, source.getClass(), type);
        MappingContext     scopedContext = invocation.context();
        MappingStrategy<T> strategy      = scopedContext.strategyRegistry().strategyFor(source, typedValue, scopedContext);

        try {
            guardDepth(scopedContext);

            // ⚠️ Guarded, and deliberately so. This runs once per property of every mapped object,
            // and getSimpleName() is an argument - it would be built on every call, disabled level
            // or not. Unguarded, this line alone was 40% of the engine's time under a console
            // appender.
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Mapping strategy: {}", strategy.getClass().getSimpleName());
            }

            T value = strategy.execute(source, typedValue, scopedContext);
            return invocation.finish(source, value, type);
        } catch (MappingException mappingException) {
            invocation.recover(locate(mappingException, scopedContext));
            return null;
        }
    }

    /**
     * Refuse a mapping that has nested further than {@link MappingConfig#maxDepth()} allows.
     *
     * <p>Sits at the one place every nested mapping passes through, so it answers for every shape -
     * beans, maps, collections and arrays alike - rather than only the ones that track references.
     * The alternative is a {@link StackOverflowError} thrown from wherever the stack happened to run
     * out, naming neither the property nor the types that got there.</p>
     *
     * <p>⚠️ Depth is counted by {@link MappingScope} as nested steps are taken, and is deliberately
     * not the length of the property path any more. The two agree everywhere except one case - a step
     * named by a map key that itself contains a dot contributes one step and two path segments - and
     * where they disagree, steps are what this limit is about. Asking the path for its length also
     * meant building the path, once per mapped object, for a number.</p>
     *
     * @param context context of this invocation
     * @throws MappingException when the limit is passed, located at the path that reached it
     */
    private void guardDepth(MappingContext context) {
        int depth   = context.scope().depth();
        int maximum = context.config().maxDepth();

        if (depth > maximum) {
            throw new MappingException(
                    ErrorCodes.MAPPING_DEPTH_EXCEEDS,
                    "Mapping nested deeper than maxDepth=%d. A cycle the reference policy does not cover, or a graph that genuinely runs this deep."
                            .formatted(maximum)
            ).withPath(context.scope().path());
        }
    }

    /**
     * Give a failure a location when the site that raised it recorded none.
     *
     * @param exception failure to locate
     * @param context context of this invocation
     * @return the same exception, or a located copy of it
     */
    private MappingException locate(MappingException exception, MappingContext context) {
        if (exception.path() != null) {
            return exception;
        }

        return exception.withPath(context.scope().path());
    }
}
