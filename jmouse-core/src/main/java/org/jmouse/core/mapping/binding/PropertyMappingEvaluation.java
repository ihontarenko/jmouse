package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ValueNavigator;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.errors.MappingException;

import java.util.function.Supplier;

public final class PropertyMappingEvaluation implements PropertyMappingVisitor<Object> {

    public static final Object IGNORED = new Object();

    private final ObjectAccessor   accessor;
    private final MappingContext   context;
    private final Supplier<Object> fallback;
    private final ValueNavigator   navigator;

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

    private Object source() {
        return accessor.unwrap();
    }

    @Override
    public Object visit(PropertyMapping.Ignore mapping) {
        return IGNORED;
    }

    @Override
    public Object visit(PropertyMapping.Constant mapping) {
        return mapping.value();
    }

    @Override
    public Object visit(PropertyMapping.Reference mapping) {
        return safeNavigate(mapping.sourceReference());
    }

    @Override
    public Object visit(PropertyMapping.Provider mapping) {
        return mapping.valueProvider().provide(source());
    }

    @Override
    public Object visit(PropertyMapping.Compute mapping) {
        return mapping.function().compute(source(), context);
    }

    @Override
    public Object visit(PropertyMapping.DefaultValue mapping) {
        Object value = mapping.delegate().accept(this);
        return (value == null ? mapping.defaultSupplier().get() : value);
    }

    @Override
    public Object visit(PropertyMapping.Transform mapping) {
        Object value = mapping.delegate().accept(this);
        return (value == null ? null : mapping.transformer().transform(value, context));
    }

    @Override
    public Object visit(PropertyMapping.When mapping) {
        return mapping.condition().test(source(), context) ? mapping.delegate().accept(this) : null;
    }

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

    @Override
    public Object visit(PropertyMapping.Required mapping) {
        Object value = mapping.delegate().accept(this);
        if (value == null) {
            throw new MappingException(mapping.errorCode(), mapping.message(), null);
        }
        return value;
    }

    private Object safeNavigate(String path) {
        Object value = navigator.navigate(accessor, path);
        return value == null ? fallback.get() : value;
    }
}
