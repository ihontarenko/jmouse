package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.bindings.BindingType;
import org.jmouse.core.mapping.bindings.FieldBinding;
import org.jmouse.core.mapping.bindings.TypeMappingBindings;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public abstract class AbstractMappingPlan<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMappingPlan.class);

    protected final InferredType targetType;

    protected AbstractMappingPlan(InferredType targetType) {
        this.targetType = Objects.requireNonNull(targetType, "targetType");
    }

    protected final ObjectAccessor sourceAccessor(Object source, MappingContext context) {
        return context.wrapper().wrap(source);
    }

    protected final Object applyBindingIfAny(
            Object source,
            ObjectAccessor accessor,
            MappingContext context,
            String targetName,
            TypeMappingBindings bindings,
            ValueSupplier fallback
    ) {
        FieldBinding binding = (bindings != null ? bindings.find(targetName) : null);

        if (binding == null) {
            return fallback.get();
        }

        BindingType type = binding.type();

        LOGGER.debug("Applying binding-type: {} for field: {}", type, targetName);

        return switch (type) {
            case IGNORE -> IgnoredValue.INSTANCE;
            case CONSTANT -> binding.constantValue();
            case COMPUTE -> binding.computeFunction().compute(source, context);
            case BIND -> {
                if (binding.valueProvider() != null) {
                    yield binding.valueProvider().provide(source);
                }

                if (binding.sourceReference() != null) {
                    yield safeNavigate(accessor, binding.sourceReference());
                }

                yield safeGet(accessor, targetName);
            }
        };
    }

    protected final Object adapt(Object value, InferredType targetType, MappingContext context) {
        if (value == null) {
            return null;
        }

        TypeInformation targetInfo = TypeInformation.forJavaType(targetType);

        // is convertible

        if (targetInfo.isScalar() || targetInfo.isEnum() || targetInfo.isClass()) {
            return convertIfNeeded(value, targetInfo.getClassType(), context.conversion());
        }

        return context.objectMapper().map(value, targetType.getRawType());
    }

    protected final Object safeNavigate(ObjectAccessor accessor, String path) {
        try {
            if (accessor.navigate(path) instanceof ObjectAccessor objectAccessor) {
                return objectAccessor.unwrap();
            }
            return null;
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to navigate value for path '{}'.", path, exception);
            return null;
        }
    }

    protected final Object safeGet(ObjectAccessor accessor, String name) {
        try {
            if (accessor.get(name) instanceof ObjectAccessor objectAccessor) {
                return objectAccessor.unwrap();
            }
            return null;
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to locate value for field '{}'.", name, exception);
            return null;
        }
    }

    protected final Object convertIfNeeded(Object value, Class<?> targetType, Conversion conversion) {
        if (value == null || targetType == null) {
            return value;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        return conversion.convert(value, targetType);
    }

    protected final MappingException fail(String code, String message, Exception exception) {
        return new MappingException(code, message, exception);
    }

    @FunctionalInterface
    public interface ValueSupplier {
        Object get();
    }

    protected enum IgnoredValue { INSTANCE }
}
