package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.binding.PropertyMapping;
import org.jmouse.core.mapping.binding.TypeMappingSpecification;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.runtime.Mapper;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Base implementation for {@link MappingPlan} that provides common utilities for
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
public abstract class AbstractMappingPlan<T> implements MappingPlan<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMappingPlan.class);

    /**
     * Target type metadata of this plan.
     */
    protected final InferredType targetType;

    /**
     * Create a mapping plan for the given target type.
     *
     * @param targetType inferred target type (never {@code null})
     * @throws NullPointerException if {@code targetType} is {@code null}
     */
    protected AbstractMappingPlan(InferredType targetType) {
        this.targetType = Objects.requireNonNull(targetType, "targetType");
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
    protected final ObjectAccessor sourceAccessor(Object source, MappingContext context) {
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
     *   <li>{@link PropertyMapping.Provider} evaluates {@code provider.provide(source)}</li>
     *   <li>{@link PropertyMapping.Reference} resolves value via {@link #safeNavigate(ObjectAccessor, String)}</li>
     *   <li>No binding returns {@code fallback.get()}</li>
     * </ul>
     *
     * @param accessor source accessor used for navigation and fallback reads
     * @param context mapping context providing runtime services
     * @param targetName target property name to resolve
     * @param bindings optional mapping rule; may be {@code null}
     * @param fallback fallback supplier when no binding exists
     * @return resolved value, {@code null}, or {@link IgnoredValue#INSTANCE} when ignored
     */
    protected final Object applyPropertyBinding(
            ObjectAccessor accessor,
            MappingContext context,
            String targetName,
            TypeMappingSpecification bindings,
            ValueSupplier fallback
    ) {
        Object          source  = accessor.unwrap();
        PropertyMapping binding = (bindings != null ? bindings.find(targetName) : null);
        return switch (binding) {
            case PropertyMapping.Ignore ignore -> IgnoredValue.INSTANCE;
            case PropertyMapping.Constant constant -> constant.value();
            case PropertyMapping.Compute compute -> compute.function().compute(source, context);
            case PropertyMapping.Provider provider -> provider.provider().provide(source);
            case PropertyMapping.Reference reference -> safeNavigate(accessor, reference.sourceReference());
            case null -> fallback.get();
        };
    }

    /**
     * Adapt the given {@code value} into {@code targetType}.
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
     * @param targetType inferred target type metadata
     * @param context mapping context providing {@link Mapper} and {@link Conversion}
     * @return adapted value (may be {@code null})
     */
    protected final Object adaptValue(Object value, InferredType targetType, MappingContext context) {
        if (value == null) {
            return null;
        }

        Mapper     mapper     = context.mapper();
        Class<?>   type       = targetType.getClassType();
        Conversion conversion = context.conversion();

        if (targetType.isScalar() || targetType.isEnum() || targetType.isClass()) {
            return convertIfNeeded(value, type, conversion);
        }

        if (conversion.hasConverter(value.getClass(), type)) {
            return convertIfNeeded(value, type, conversion);
        }

        if (type.isInstance(value)) {
            return value;
        }

        return mapper.map(value, targetType);
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
            LOGGER.error("Unable to locate value for field '{}'.", name, exception);
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
