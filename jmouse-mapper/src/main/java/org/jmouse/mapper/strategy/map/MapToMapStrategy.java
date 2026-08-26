package org.jmouse.mapper.strategy.map;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.config.MappingPolicy;
import org.jmouse.mapper.config.TypeMismatchPolicy;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.mapper.strategy.support.AbstractMapStrategy;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.MappingDestination;
import org.jmouse.core.reflection.InferredType;

import java.util.Map;

/**
 * Mapping strategy for {@code Map -> Map} transformations. 🗺️
 *
 * <p>This strategy reads entries from a source map-like value and adapts both keys and values into
 * the target map key/value types described by {@link TypedValue#getType()}.</p>
 *
 * <h3>Nested values</h3>
 * <p>If the declared target value type is {@code Object} and the source value is not simple,
 * the value is materialized as a nested {@code Map<Object,Object>} using {@link #NESTED_MAP_TYPE}.</p>
 *
 * <h3>Error handling</h3>
 * <p>Entry adaptation errors are handled according to {@link MappingPolicy#typeMismatchPolicy()}:</p>
 * <ul>
 *   <li>{@link TypeMismatchPolicy#FAIL}: throw a {@code map.entry_adapt_failed} exception</li>
 *   <li>otherwise: skip the failing entry</li>
 * </ul>
 *
 * <p>When a key is a {@link String}, the mapping path is temporarily extended to improve diagnostics.</p>
 */
public final class MapToMapStrategy extends AbstractMapStrategy<Map<Object, Object>> {

    /**
     * Nested materialization type used when the target value type is {@code Object}
     * and the source value is complex (non-simple).
     */
    private static final InferredType NESTED_MAP_TYPE = InferredType.forParametrizedClass(
            Map.class, Object.class, Object.class
    );

    /**
     * Execute map-to-map mapping.
     *
     * @param source source value (expected to be map-like)
     * @param typedValue typed target descriptor (target map type + optional instance)
     * @param context mapping context
     * @return mapped target map, or {@code null} when {@code source} is {@code null} or not map-like
     */
    @Override
    public Map<Object, Object> execute(Object source, TypedValue<Map<Object, Object>> typedValue, MappingContext context) {
        if (source == null) {
            return null;
        }

        MappingPolicy policy = context.policy();

        InferredType mapType   = typedValue.getType().toMap();
        InferredType valueType = mapType.getLast();
        InferredType keyType   = mapType.getFirst();

        Map<Object, Object> target   = getTargetMap(context.config(), typedValue);
        ObjectAccessor      accessor = toObjectAccessor(source, context);

        if (!accessor.isMap()) {
            return null;
        }

        for (Object keyValue : accessor.keySet()) {
            ObjectAccessor objectAccessor = accessor.get(keyValue);
            Object         mapValue       = objectAccessor.unwrap();

            InferredType effectiveType = valueType;

            if (effectiveType.isObject() && !objectAccessor.isSimple()) {
                effectiveType = NESTED_MAP_TYPE;
            }

            try {
                keyValue = adaptValue(keyValue, keyType, context);

                MappingContext temporaryContext = context;

                if (keyValue instanceof String keyString) {
                    temporaryContext = context.appendPath(keyString);
                }

                MappingDestination destination = pluginsActive(temporaryContext)
                        ? new MappingDestination.MapEntry(
                                target, temporaryContext.currentPath(), keyValue, effectiveType)
                        : null;

                mapValue = adaptValue(
                        mapValue,
                        getTypedValue(temporaryContext, toObjectAccessor(target, context), keyValue, effectiveType),
                        temporaryContext,
                        destination
                );
            } catch (RuntimeException exception) {
                if (policy.typeMismatchPolicy() == TypeMismatchPolicy.FAIL) {
                    throw toMappingException(
                            context,
                            ErrorCodes.MAP_ENTRY_ADAPT_FAILED,
                            "Failed to adapt map entry key/value to target map types", exception
                    );
                }
                continue;
            }

            target.put(keyValue, mapValue);
        }

        return target;
    }

}
