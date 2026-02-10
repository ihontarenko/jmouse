package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ValueNavigator;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.errors.MappingException;

import java.util.function.Supplier;

/**
 * Runtime evaluator for {@link PropertyMapping} definitions. ⚙️
 *
 * <p>{@code PropertyMappingEvaluation} walks a {@link PropertyMapping} tree using the
 * {@link PropertyMappingVisitor} pattern and produces a concrete value for a target property.</p>
 *
 * <p>Evaluation is performed against:</p>
 * <ul>
 *   <li>an {@link ObjectAccessor} (source value wrapper)</li>
 *   <li>a {@link MappingContext} (conversion, mapper, policies, path, etc.)</li>
 *   <li>a {@link ValueNavigator} (path navigation engine)</li>
 *   <li>a fallback supplier used when navigation returns {@code null}</li>
 * </ul>
 *
 * <p>Special sentinel value {@link #IGNORED} is returned for {@link PropertyMapping.Ignore}
 * and should be interpreted by callers as "do not write this property".</p>
 *
 * <p>This evaluator is stateless relative to mapping definitions and can be reused per evaluation scope.</p>
 */
public final class PropertyMappingEvaluation implements PropertyMappingVisitor<Object> {

    /**
     * Sentinel object used to signal that the property should be ignored.
     */
    public static final Object IGNORED = new Object();

    private final ObjectAccessor   accessor;
    private final MappingContext   context;
    private final Supplier<Object> fallback;
    private final ValueNavigator   navigator;

    /**
     * Create a new mapping evaluator.
     *
     * @param accessor source accessor (never {@code null})
     * @param context mapping context (never {@code null})
     * @param navigator navigation engine (never {@code null})
     * @param fallback fallback value supplier used when navigation returns {@code null}
     */
    public PropertyMappingEvaluation(
            ObjectAccessor accessor,
            MappingContext context,
            ValueNavigator navigator,
            Supplier<Object> fallback
    ) {
        this.accessor = Verify.nonNull(accessor, "accessor");
        this.context = Verify.nonNull(context, "context");
        this.navigator = Verify.nonNull(navigator, "navigator");
        this.fallback = Verify.nonNull(fallback, "fallback");
    }

    /**
     * Unwrapped source object.
     *
     * @return raw source value
     */
    private Object source() {
        return accessor.unwrap();
    }

    /**
     * Ignore mapping evaluation.
     *
     * @param mapping ignore mapping
     * @return {@link #IGNORED} sentinel
     */
    @Override
    public Object visit(PropertyMapping.Ignore mapping) {
        return IGNORED;
    }

    /**
     * Constant mapping evaluation.
     *
     * @param mapping constant mapping
     * @return constant value
     */
    @Override
    public Object visit(PropertyMapping.Constant mapping) {
        return mapping.value();
    }

    /**
     * Reference mapping evaluation using {@link ValueNavigator}.
     *
     * @param mapping reference mapping
     * @return resolved value or fallback when navigation returns {@code null}
     */
    @Override
    public Object visit(PropertyMapping.Reference mapping) {
        return safeNavigate(mapping.sourceReference());
    }

    /**
     * Provider mapping evaluation.
     *
     * @param mapping provider mapping
     * @return provider result
     */
    @Override
    public Object visit(PropertyMapping.Provider mapping) {
        return mapping.valueProvider().provide(source());
    }

    /**
     * Compute mapping evaluation.
     *
     * @param mapping compute mapping
     * @return computed value
     */
    @Override
    public Object visit(PropertyMapping.Compute mapping) {
        return mapping.function().compute(source(), context);
    }

    /**
     * Default value decorator evaluation.
     *
     * @param mapping default value mapping
     * @return delegate value or default when delegate returns {@code null}
     */
    @Override
    public Object visit(PropertyMapping.DefaultValue mapping) {
        Object value = mapping.delegate().accept(this);
        return (value == null ? mapping.defaultSupplier().get() : value);
    }

    /**
     * Transform decorator evaluation.
     *
     * @param mapping transform mapping
     * @return transformed delegate value or {@code null}
     */
    @Override
    public Object visit(PropertyMapping.Transform mapping) {
        Object value = mapping.delegate().accept(this);
        return (value == null ? null : mapping.transformer().transform(value, context));
    }

    /**
     * Conditional mapping evaluation.
     *
     * @param mapping conditional mapping
     * @return delegate value when condition matches, otherwise {@code null}
     */
    @Override
    public Object visit(PropertyMapping.When mapping) {
        return mapping.condition().test(source(), context)
                ? mapping.delegate().accept(this)
                : null;
    }

    /**
     * Coalesce mapping evaluation.
     *
     * @param mapping coalesce mapping
     * @return first non-null candidate result, or {@code null}
     */
    @Override
    public Object visit(PropertyMapping.Coalesce mapping) {
        Object value = null;
        for (PropertyMapping candidate : mapping.candidates()) {
            value = candidate.accept(this);
            if (value != null) {
                break;
            }
        }
        return value;
    }

    /**
     * Required mapping evaluation.
     *
     * @param mapping required mapping
     * @return delegate value
     * @throws MappingException when delegate produces {@code null}
     */
    @Override
    public Object visit(PropertyMapping.Required mapping) {
        Object value = mapping.delegate().accept(this);
        if (value == null) {
            throw new MappingException(mapping.errorCode(), mapping.message(), null);
        }
        return value;
    }

    /**
     * Safely navigate a source path using {@link ValueNavigator}.
     *
     * <p>When navigation result is {@code null}, fallback supplier is used.</p>
     *
     * @param path navigation path
     * @return resolved or fallback value
     */
    private Object safeNavigate(String path) {
        Object value = navigator.navigate(accessor, path);
        return value == null ? fallback.get() : value;
    }
}
