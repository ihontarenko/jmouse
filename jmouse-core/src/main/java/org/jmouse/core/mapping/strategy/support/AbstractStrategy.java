package org.jmouse.core.mapping.strategy.support;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.MappingScope;
import org.jmouse.core.mapping.binding.PropertyMapping;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.plugin.MappingValue;
import org.jmouse.core.mapping.plugin.PluginBus;
import org.jmouse.core.reflection.InferredType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Base implementation for {@link MappingStrategy} that provides common utilities for
 * mapping plan execution. 🧠
 *
 * <p>This abstraction centralizes reusable plan logic such as:</p>
 * <ul>
 *   <li>Keeping the resolved {@link InferredType} of the target</li>
 *   <li>Wrapping source objects into an {@link ObjectAccessor} using {@link MappingContext}</li>
 *   <li>Shared helper methods for binding resolution, safe access, and value adaptation (in subclasses)</li>
 * </ul>
 *
 * <p>Concrete plan implementations are responsible for the actual mapping process
 * (constructing the target object and populating its fields/properties).</p>
 *
 * @param <T> target type produced by the mapping plan
 */
public abstract class AbstractStrategy<T> implements MappingStrategy<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStrategy.class);

    public TypedValue<?> getTypedValue(ObjectAccessor accessor, Object key, InferredType type) {
        TypedValue<Object> typedValue  = TypedValue.of(type);
        Object             targetValue = accessor.get(key);

        if (targetValue != null) {
            typedValue = TypedValue.of(InferredType.forInstance(targetValue));
            typedValue = typedValue.withInstance(targetValue);
        }

        return typedValue;
    }

    /**
     * Wrap the given {@code source} object into an {@link ObjectAccessor} using the wrapper
     * provided by the current {@link MappingContext}.
     *
     * <p>The returned accessor abstracts over the underlying source structure
     * (bean, map, record, structured node, etc.).</p>
     *
     * @param source source object to wrap (may be {@code null}, depending on wrapper behavior)
     * @param context mapping context providing wrapper configuration
     * @return accessor for the given source
     */
    protected final ObjectAccessor toObjectAccessor(Object source, MappingContext context) {
        return context.wrapper().wrap(source);
    }

    /**
     * Apply a {@link PropertyMapping} for {@code targetName} if present in {@code bindings},
     * otherwise use {@code fallback}.
     *
     * <p>Resolution rules:</p>
     * <ul>
     *   <li>{@link PropertyMapping.Ignore} returns {@link IgnoredValue#INSTANCE}</li>
     *   <li>{@link PropertyMapping.Constant} returns the constant value</li>
     *   <li>{@link PropertyMapping.Compute} evaluates {@code function.compute(source, context)}</li>
     *   <li>{@link PropertyMapping.Provider} evaluates {@code valueProvider.provide(source)}</li>
     *   <li>{@link PropertyMapping.Reference} resolves value via {@link #safeNavigate(ObjectAccessor, String)}</li>
     *   <li>No binding returns {@code fallback.get()}</li>
     * </ul>
     *
     * @param accessor source accessor used for navigation and fallback reads
     * @param context mapping context providing runtime services
     * @param fallback fallback supplier when no binding exists
     * @return resolved value, {@code null}, or {@link IgnoredValue#INSTANCE} when ignored
     */
    protected final Object applyValue(
            ObjectAccessor accessor,
            MappingContext context,
            PropertyMapping mapping,
            ValueSupplier fallback
    ) {
        Object source = accessor.unwrap();
        return switch (mapping) {
            case PropertyMapping.Ignore ignore -> IgnoredValue.INSTANCE;
            case PropertyMapping.Constant constant -> constant.value();
            case PropertyMapping.Compute compute -> compute.function().compute(source, context);
            case PropertyMapping.Reference reference -> safeNavigate(accessor, reference.sourceReference());
            case PropertyMapping.Provider provider -> provider.valueProvider().provide(source);
            case null -> fallback.get();
        };
    }

    /**
     * Convenience overload for adapting {@code value} into the given {@code targetType}.
     *
     * @param value raw value to adapt (may be {@code null})
     * @param targetType inferred target type
     * @param context mapping context
     * @return adapted value (may be {@code null})
     */
    protected final Object adaptValue(Object value, InferredType targetType, MappingContext context) {
        return adaptValue(value, TypedValue.of(targetType), context);
    }

    /**
     * Adapt the given {@code value} into the target described by {@code typedValue}.
     *
     * <p>Strategy:</p>
     * <ul>
     *   <li>{@code null} input returns {@code null}</li>
     *   <li>Scalar/enum/plain-class targets are converted via {@link Conversion}</li>
     *   <li>If a dedicated converter exists for {@code value -> targetType}, conversion is preferred</li>
     *   <li>Otherwise mapping is delegated to {@link Mapper} for deep object mapping</li>
     * </ul>
     *
     * @param value raw value to adapt (may be {@code null})
     * @param typedValue target type metadata and optional target instance holder
     * @param context mapping context providing {@link Mapper} and {@link Conversion}
     * @return adapted value (may be {@code null})
     */
    protected final Object adaptValue(Object value, TypedValue<?> typedValue, MappingContext context) {
        if (value == null) {
            return null;
        }

        InferredType targetType = typedValue.getType();
        PluginBus    bus        = context.plugins();
        Mapper       mapper     = context.mapper();
        Class<?>     type       = targetType.getClassType();
        Conversion   conversion = context.conversion();

        MappingScope scope = context.scope();
        Object       root  = scope.root();
        PropertyPath path  = scope.path();

        value = bus.onValue(new MappingValue(
                root,
                value,
                targetType,
                path,
                context
        ));

        if (targetType.isScalar() || targetType.isEnum() || targetType.isClass()) {
            return mapper.map(value, typedValue);
        }

        if (hasConverterFor(value.getClass(), type, context) && !targetType.isCollection() && !targetType.isMap()) {
            return convertIfNeeded(value, type, conversion);
        }

        if (type.isInstance(value) && !targetType.isCollection() && !targetType.isMap()) {
            return value;
        }

        return mapper.map(value, typedValue);
    }

    /**
     * Safely navigate a nested path via {@link ObjectAccessor#navigate(String)}.
     *
     * <p>Any runtime failure is caught, logged, and converted to {@code null}.</p>
     *
     * @param accessor source accessor
     * @param path navigation path (syntax depends on accessor implementation)
     * @return navigated value or {@code null} when missing or on error
     */
    protected final Object safeNavigate(ObjectAccessor accessor, String path) {
        try {
            PropertyPath propertyPath = PropertyPath.forPath(path);

            if (propertyPath.isSimple()) {
                return safeGet(accessor, path);
            }

            if (accessor.navigate(propertyPath) instanceof ObjectAccessor objectAccessor) {
                return objectAccessor.unwrap();
            }

            return null;
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to navigate value for path '{}'.", path, exception);
            return null;
        }
    }

    /**
     * Safely read a direct property/field via {@link ObjectAccessor#get(String)}.
     *
     * <p>Any runtime failure is caught, logged, and converted to {@code null}.</p>
     *
     * @param accessor source accessor
     * @param name property/field name
     * @return value or {@code null} when missing or on error
     */
    protected final Object safeGet(ObjectAccessor accessor, String name) {
        try {
            if (accessor.get(name) instanceof ObjectAccessor objectAccessor) {
                return objectAccessor.unwrap();
            }
            return null;
        } catch (RuntimeException exception) {
            LOGGER.error("Getting safety value for '{}' failed. Cause: {}", name, exception.getMessage());
            return null;
        }
    }

    /**
     * Convert {@code value} to {@code targetType} if required.
     *
     * <p>Conversion rules:</p>
     * <ul>
     *   <li>If {@code value} is {@code null} - returns {@code null}</li>
     *   <li>If {@code targetType} is {@code null} - returns {@code value} unchanged</li>
     *   <li>If {@code value} is already an instance of {@code targetType} - returns {@code value} unchanged</li>
     *   <li>Otherwise delegates to {@link Conversion#convert(Object, Class)}</li>
     * </ul>
     *
     * @param value value to convert (may be {@code null})
     * @param targetType desired runtime target class (may be {@code null})
     * @param conversion conversion service
     * @return converted value, or original value when conversion is not needed
     */
    protected final Object convertIfNeeded(Object value, Class<?> targetType, Conversion conversion) {
        if (value == null || targetType == null) {
            return value;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        return conversion.convert(value, targetType);
    }

    /**
     * Check whether the conversion subsystem can produce a value of {@code targetType} from {@code sourceType}.
     *
     * <p>This method delegates to {@link org.jmouse.core.convert.Conversion#hasAnyConverter(Class, Class)},
     * which may consider both direct converters and multi-step conversion chains.</p>
     *
     * @param sourceType source runtime type
     * @param targetType target runtime type
     * @param context mapping context providing the {@link org.jmouse.core.convert.Conversion} service
     * @return {@code true} if any conversion path exists, otherwise {@code false}
     */
    protected final boolean hasConverterFor(Class<?> sourceType, Class<?> targetType, MappingContext context) {
        return context.conversion().hasAnyConverter(sourceType, targetType);
    }

    /**
     * Create a {@link MappingException} with the provided error parameters.
     *
     * @param code stable error code identifier
     * @param message human-readable error message
     * @param exception underlying cause (may be {@code null})
     * @return new {@link MappingException}
     */
    protected final MappingException toMappingException(String code, String message, Exception exception) {
        return new MappingException(code, message, exception);
    }

    /**
     * Lazy fallback supplier used when no explicit {@link PropertyMapping}
     * exists for a target property.
     */
    @FunctionalInterface
    public interface ValueSupplier {

        /**
         * Compute and return a fallback value.
         *
         * @return fallback value (may be {@code null})
         */
        Object get();
    }

    /**
     * Sentinel marker used to indicate that a target property was explicitly ignored.
     *
     * <p>This is distinct from {@code null}, which may represent a legitimate mapped value.</p>
     */
    protected enum IgnoredValue { INSTANCE }
}
