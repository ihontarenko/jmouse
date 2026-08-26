package org.jmouse.mapper.strategy.support;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.convert.Conversion;
import org.jmouse.mapper.MappingDestination;
import org.jmouse.mapper.MappingScope;
import org.jmouse.mapper.binding.PropertyMapping;
import org.jmouse.mapper.binding.PropertyMappingEvaluation;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.mapper.errors.MappingException;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.TargetShape;
import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.plugin.MappingValue;
import org.jmouse.mapper.plugin.PluginBus;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.helpers.Arrays;
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

    /**
     * Resolve a {@link TypedValue} for mapping a value stored under {@code key} inside the given accessor.
     *
     * <p>This helper is used primarily by map/collection strategies to support "in-place" mapping:
     * if a target container already contains a value for the given {@code key}, that value can be reused
     * as the target instance.</p>
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Starts with {@code TypedValue.of(type)} (no instance).</li>
     *   <li>Reads {@code accessor.get(key)} and, if present and not null, unwraps it.</li>
     *   <li>Performs a compatibility check: if the existing value is not array/collection and is not
     *       assignable to the requested {@code type}, a {@link MappingException}
     *       is thrown.</li>
     *   <li>On success, returns {@code typedValue.withInstance(unwrapped)}.</li>
     * </ul>
     *
     * @param accessor source/target accessor used to locate the existing value
     * @param key key/index/property used to lookup the nested value
     * @param type expected inferred type for the nested value
     * @return typed value describing {@code type} and optionally carrying an existing instance
     */
    public TypedValue<?> getTypedValue(MappingContext context, ObjectAccessor accessor, Object key, InferredType type) {
        TypedValue<Object> typedValue     = TypedValue.of(type);
        ObjectAccessor     objectAccessor = accessor.get(key);

        if (objectAccessor != null && !objectAccessor.isNull()) {
            Object unwrapped = objectAccessor.unwrap();

            InferredType effectiveType = type;

            if (effectiveType.isPrimitive()) {
                effectiveType = InferredType.forType(Arrays.boxType(effectiveType.getClassType()));
            }

            if (!objectAccessor.isArray() && !objectAccessor.isCollection() && !objectAccessor.is(effectiveType)) {
                throw toMappingException(
                        context,
                        ErrorCodes.STRATEGY_INCOMPATIBLE_TYPE,
                        "Incompatible type of value %s, required-type: %s".formatted(
                                objectAccessor.getClassType(), type.getClassType()), null
                );
            }

            typedValue = typedValue.withInstance(unwrapped);
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
        if (mapping == null) {
            return fallback.get();
        }

        Object value = mapping.accept(new PropertyMappingEvaluation(accessor, context, this::safeNavigate, fallback::get));

        if (value == PropertyMappingEvaluation.IGNORED) {
            return IgnoredValue.INSTANCE;
        }

        return value;
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
     * Adapt {@code value} without naming a destination.
     *
     * <p>Used where the target does not exist yet - a record component collected before the record is
     * constructed, an array element gathered into a temporary list. Plugins receive a
     * {@link MappingDestination.RootTarget} with a {@code null} target, which says "no object to
     * point at yet" rather than pointing at the wrong one.</p>
     *
     * @param value raw value to adapt (may be {@code null})
     * @param typedValue target type metadata and optional target instance holder
     * @param context mapping context
     * @return adapted value (may be {@code null})
     */
    protected final Object adaptValue(Object value, TypedValue<?> typedValue, MappingContext context) {
        return adaptValue(value, typedValue, context, null);
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
    protected final Object adaptValue(Object value, TypedValue<?> typedValue, MappingContext context, MappingDestination destination) {
        return adaptValue(value, typedValue, context, destination, null);
    }

    /**
     * Adapt the given {@code value} into the target described by {@code typedValue}, with the target's
     * shape already known.
     *
     * <p>A caller that resolves its targets once per type pair - a compiled plan - passes the
     * {@link TargetShape} it kept, and the classification predicates are not run at all. Passing
     * {@code null} classifies on the spot and behaves exactly as the overload above.</p>
     *
     * @param value raw value to adapt (may be {@code null})
     * @param typedValue target type metadata and optional target instance holder
     * @param context mapping context providing {@link Mapper} and {@link Conversion}
     * @param destination the slot being written, or {@code null} when nothing is listening
     * @param shape what the target type is, or {@code null} to work it out here
     * @return adapted value (may be {@code null})
     */
    protected final Object adaptValue(
            Object value,
            TypedValue<?> typedValue,
            MappingContext context,
            MappingDestination destination,
            TargetShape shape
    ) {
        if (value == null) {
            return null;
        }

        InferredType targetType = typedValue.getType();
        PluginBus    bus        = context.plugins();

        // ⚠️ Guarded, and the guard is the point rather than a micro-optimization. Everything the
        // pipeline is handed - the value wrapper, the destination, and the property path both of them
        // carry - is built per property of every mapped object, and building the path is parsing,
        // a cache lookup and an entry merge. With no plugins registered, which is the ordinary
        // configuration, all of it was constructed and discarded unread.
        if (bus.isActive()) {
            MappingScope scope = context.scope();
            PropertyPath path  = scope.path();

            if (destination == null) {
                destination = new MappingDestination.RootTarget(null, path);
            }

            value = bus.onValue(new MappingValue(
                    scope.sourceRoot(),
                    value,
                    targetType,
                    path,
                    context,
                    destination
            ));
        }

        TargetShape targetShape = shape == null ? TargetShape.of(targetType) : shape;
        Mapper      mapper      = context.mapper();
        Class<?>    type        = targetShape.rawType();

        if (targetShape.scalarLike()) {
            return mapper.map(value, typedValue);
        }

        // ⚠️ The structure test comes first now. It used to trail the converter lookup, which meant
        // every collection, map and array searched the conversion graph for a converter it was then
        // told it could not use. Both are pure predicates, so the order is free to choose.
        if (!targetShape.complexStructure() && hasConverterFor(value.getClass(), type, context)) {
            return convertIfNeeded(value, type, context.conversion());
        }

        if (!targetShape.container() && type.isInstance(value)) {
            return value;
        }

        return mapper.map(value, typedValue);
    }

    /**
     * Whether a {@link MappingDestination} is worth building at all.
     *
     * <p>A destination exists to be handed to plugins and is read by nothing else, so with none
     * registered it is an allocation and a property path built to be discarded. Call sites ask this
     * before constructing one and pass {@code null} when the answer is no.</p>
     *
     * @param context mapping context holding the bus
     * @return {@code true} when at least one plugin will see the destination
     */
    protected final boolean pluginsActive(MappingContext context) {
        return context.plugins().isActive();
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
            // ⚠️ ONE call, and it carries both questions. "The source has no such property" is the
            // ordinary case in mapping rather than a fault - a target routinely carries fields its source
            // does not - so it must not be learned from a thrown exception, which costs a stack trace and
            // a formatted message per property per object. But asking hasProperty and then read probed
            // the SAME map twice, once per property of every object; on a flat bean-to-bean mapping that
            // pair outweighed the generated getter and setter combined. readIfPresent answers both from
            // one lookup, and an accessor that cannot tell falls back to exactly the old two questions.
            //
            // ⚠️ read, not get: get answers with an accessor, and this line used to unwrap it on the
            // spot - one allocation per property of every mapped object, built only to be opened.
            return accessor.readIfPresent(name);
        } catch (RuntimeException exception) {
            LOGGER.warn("Getting safety value for '{}' failed. Cause: {}", name, exception.getMessage());
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

        // ⚠️ Box first. `boolean.class.isInstance(Boolean.TRUE)` is false - a primitive class is an
        // instance of nothing - so an unboxed check sends every primitive property through the whole
        // conversion machinery, graph search and swallowed ConverterNotFound included, to arrive at
        // the value it already had.
        Class<?> effectiveType = targetType.isPrimitive() ? Arrays.boxType(targetType) : targetType;

        if (effectiveType.isInstance(value)) {
            return value;
        }

        return conversion.convert(value, effectiveType);
    }

    /**
     * Check whether the conversion subsystem can produce a value of {@code targetType} from {@code sourceType}.
     *
     * <p>This method delegates to {@link Conversion#hasAnyConverter(Class, Class)},
     * which may consider both direct converters and multi-step conversion chains.</p>
     *
     * @param sourceType source runtime type
     * @param targetType target runtime type
     * @param context mapping context providing the {@link Conversion} service
     * @return {@code true} if any conversion path exists, otherwise {@code false}
     */
    protected final boolean hasConverterFor(Class<?> sourceType, Class<?> targetType, MappingContext context) {
        return context.conversion().hasAnyConverter(sourceType, targetType);
    }

    /**
     * Create a {@link MappingException} located at the context's current path.
     *
     * <p>Stamping the location here rather than where the exception is caught is what makes the
     * reported path the property that actually failed, instead of the root of the mapping.</p>
     *
     * @param context mapping context supplying the current path
     * @param code stable error code identifier
     * @param message human-readable error message
     * @param exception underlying cause (may be {@code null})
     * @return new {@link MappingException}
     */
    protected final MappingException toMappingException(
            MappingContext context,
            String code,
            String message,
            Exception exception
    ) {
        return new MappingException(code, message, exception, Map.of(), context.currentPath());
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
