package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.errors.ErrorAction;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plugin.*;
import org.jmouse.core.reflection.InferredType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates a single mapping invocation and its plugin lifecycle. 🧭
 *
 * <p>{@code MappingInvocation} is responsible for:</p>
 * <ul>
 *   <li>Determining whether the invocation is the root mapping call</li>
 *   <li>Creating/using a scoped {@link MappingContext} (root scope initialization)</li>
 *   <li>Triggering plugin lifecycle hooks ({@code onStart}/{@code onFinish}/{@code onError})</li>
 * </ul>
 *
 * <p>Plugins are executed only for the root mapping call. Nested mapping operations use
 * {@link PluginBus.Noop} to avoid repeated lifecycle notifications.</p>
 */
public final class MappingInvocation {

    private static final Logger LOGGER = LoggerFactory.getLogger(MappingInvocation.class);

    private final MappingContext context;
    private final PluginBus      bus;
    private final boolean        root;

    private MappingInvocation(MappingContext context, PluginBus bus, boolean root) {
        this.context = Verify.nonNull(context, "context");
        this.bus = Verify.nonNull(bus, "bus");
        this.root = root;
    }

    /**
     * Begin a mapping invocation.
     *
     * <p>If the provided {@code base} context does not have a root scope yet, this method:</p>
     * <ul>
     *   <li>creates a root {@link MappingScope} using the provided {@code source}</li>
     *   <li>switches to a scoped context via {@link MappingContext#withScope(MappingScope)}</li>
     *   <li>enables plugins from {@link MappingContext#plugins()}</li>
     * </ul>
     *
     * <p>If the context already has a root scope, the invocation is considered nested and
     * plugins are disabled (noop bus).</p>
     *
     * @param base base mapping context
     * @param source root source object
     * @param sourceType declared/expected source type (used for diagnostics)
     * @param targetType target type to map into
     * @return new {@link MappingInvocation} instance
     */
    public static MappingInvocation begin(MappingContext base, Object source, Class<?> sourceType, InferredType targetType) {
        boolean root = base.scope().root() == null;

        MappingContext scoped = root
                ? base.withScope(MappingScope.root(source))
                : base;

        PluginBus bus = root ? scoped.plugins() : PluginBus.Noop.INSTANCE;

        bus.onStart(new MappingCall(source, sourceType, targetType, scoped));

        return new MappingInvocation(scoped, bus, root);
    }

    /**
     * Return the scoped mapping context associated with this invocation.
     *
     * @return mapping context
     */
    public MappingContext context() {
        return context;
    }

    /**
     * Finish the invocation successfully and notify plugins.
     *
     * @param source root source object
     * @param value produced target value
     * @param targetType target type metadata
     * @param <T> target value type
     * @return the same {@code value} for fluent use
     */
    public <T> T finish(Object source, T value, InferredType targetType) {
        bus.onFinish(new MappingResult(source, value, targetType, context));
        return value;
    }

    /**
     * Fail the invocation, notify plugins, and return the provided exception for rethrowing.
     *
     * <p>The failure payload includes the current {@link PropertyPath} derived from the mapping scope.</p>
     *
     * @param exception mapping exception
     * @return the same exception instance (as {@link RuntimeException})
     */
    public RuntimeException fail(MappingException exception) {
        PropertyPath  propertyPath = context.scope().path();
        MappingConfig config       = context.config();

        bus.onError(new MappingFailure(exception, propertyPath, context));

        switch (config.errorCodePolicy().getActionFor(exception.code())) {
            case THROW -> {
                LOGGER.error("[{}]: {}", exception.code(), exception.getMessage(), exception);
                throw exception;
            }
            case WARN -> {
                LOGGER.warn("[{}]: {} meta={}", exception.code(), exception.getMessage(), exception.meta(), exception);
                return null;
            }
            case SILENT -> {
                return null;
            }
        }

        // not reachable, but friendly for compiler
        return exception;
    }
}
