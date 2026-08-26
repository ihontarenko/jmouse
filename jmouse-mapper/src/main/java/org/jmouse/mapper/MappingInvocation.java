package org.jmouse.mapper;

import org.jmouse.core.Verify;
import org.jmouse.core.access.PropertyPath;
import org.jmouse.mapper.errors.ErrorAction;
import org.jmouse.mapper.errors.MappingException;
import org.jmouse.mapper.plugin.*;
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
 *   <li>Turning a {@link MappingException} into the action {@link org.jmouse.mapper.errors.ErrorsPolicy}
 *       prescribes for its code</li>
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
        boolean root = base.scope().sourceRoot() == null;

        MappingContext scoped = root
                ? base.withScope(MappingScope.root(source))
                : base;

        PluginBus bus = root ? scoped.plugins() : PluginBus.Noop.INSTANCE;

        // ⚠️ The call descriptor is built only when a plugin will read it. This runs once per mapped
        // object AND once per scalar property - every value adapted re-enters the mapper - so with no
        // plugins registered it was a five-field record allocated for a no-op, and profiling the flat
        // path put this method at 14% of the engine's time.
        if (bus.isActive()) {
            bus.onStart(new MappingCall(source, sourceType, targetType, scoped));
        }

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
     * Whether this invocation is the outermost one.
     *
     * @return {@code true} for the root call, {@code false} for a nested one
     */
    public boolean isRoot() {
        return root;
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
        if (bus.isActive()) {
            bus.onFinish(new MappingResult(source, value, targetType, context));
        }

        return value;
    }

    /**
     * Notify plugins of a failure and apply the configured {@link ErrorAction} for its code.
     *
     * <p>The reported location comes from the exception itself when it carries one, because the
     * scope held here is the scope this invocation <em>began</em> with - for the root call that is
     * the empty path, which is never where the failure happened.</p>
     *
     * <p>Only the invocation that decides the outcome logs. A nested call that rethrows says
     * nothing, so one failure produces one stack trace rather than one per level of nesting.</p>
     *
     * @param exception mapping exception
     * @return {@code true} when the caller should swallow the failure and return {@code null}
     * @throws MappingException when the policy for this code is {@link ErrorAction#THROW}
     */
    public boolean recover(MappingException exception) {
        PropertyPath location = locationOf(exception);
        ErrorAction  action   = context.config().errorsPolicy().getActionFor(exception.code());

        bus.onError(new MappingFailure(exception, location, context));

        switch (action) {
            case THROW -> {
                if (root) {
                    LOGGER.error("[{}] at '{}': {}", exception.code(), location, exception.getMessage(), exception);
                }
                throw exception;
            }
            case WARNING -> LOGGER.warn(
                    "[{}] at '{}': {} meta={}", exception.code(), location, exception.getMessage(), exception.meta());
            case SILENT -> LOGGER.debug(
                    "[{}] at '{}': {}", exception.code(), location, exception.getMessage());
        }

        return true;
    }

    /**
     * Where the failure happened.
     *
     * <p>Prefers the path the exception recorded at the point of failure; falls back to this
     * invocation's own scope when it carries none.</p>
     *
     * @param exception mapping exception
     * @return property path of the failure
     */
    private PropertyPath locationOf(MappingException exception) {
        PropertyPath recorded = exception.path();
        return recorded == null ? context.scope().path() : recorded;
    }
}
