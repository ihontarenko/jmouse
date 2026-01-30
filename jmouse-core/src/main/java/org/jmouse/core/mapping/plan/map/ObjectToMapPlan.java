package org.jmouse.core.mapping.plan.map;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.binding.PropertyMapping;
import org.jmouse.core.mapping.binding.TypeMappingRule;
import org.jmouse.core.mapping.config.MapKeyPolicy;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.plan.support.AbstractMapPlan;
import org.jmouse.core.reflection.InferredType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mapping plan that converts an arbitrary object into a "tree" {@code Map<String, Object>} structure. 🗺️
 *
 * <p>This plan is intended for object-to-map projections (e.g., exporting objects as structured maps).
 * The resulting map is built using:</p>
 * <ul>
 *   <li>explicit mapping rules (rename/ignore/reference/compute/constant/provider) when available</li>
 *   <li>a "rest mapping" pass that copies remaining source properties into the target map</li>
 * </ul>
 *
 * <p>Value materialization uses a recursive "tree" strategy:</p>
 * <ul>
 *   <li>scalars remain scalars</li>
 *   <li>map-like objects are mapped into {@code Map<String,Object>}</li>
 *   <li>iterables/arrays are mapped into {@code List<Object>}</li>
 *   <li>complex objects (bean/record) are mapped into {@code Map<String,Object>}</li>
 * </ul>
 *
 * <p>Key coercion for the rest mapping phase is controlled by {@link MapKeyPolicy}.</p>
 */
public final class ObjectToMapPlan extends AbstractMapPlan<Map<Object, Object>> {

    /**
     * Target type for object tree maps.
     */
    private static final InferredType TREE_MAP_TYPE =
            InferredType.forParametrizedClass(Map.class, String.class, Object.class);

    /**
     * Target type for object tree lists.
     */
    private static final InferredType TREE_LIST_TYPE =
            InferredType.forParametrizedClass(List.class, Object.class);

    /**
     * Cached inferred type for {@link String}.
     *
     * <p>Used for string-target adaptation and conversion checks.</p>
     */
    private static final InferredType STRING_TYPE =
            InferredType.forType(String.class);

    /**
     * Create a plan instance for the requested target type.
     *
     * @param typedValue typed value target map type
     */
    public ObjectToMapPlan(TypedValue<Map<Object, Object>> typedValue) {
        super(typedValue);
    }

    /**
     * Execute object-to-map mapping.
     *
     * <p>Algorithm outline:</p>
     * <ol>
     *   <li>Wrap source into {@link ObjectAccessor}.</li>
     *   <li>Instantiate target map via {@link #instantiate(MappingConfig)}.</li>
     *   <li>Apply explicit rules ({@link TypeMappingRule}) first and mark consumed source keys.</li>
     *   <li>Copy the remaining source keys into the target ("rest mapping").</li>
     * </ol>
     *
     * @param source source object
     * @param context mapping context
     * @return mapped tree map, or {@code null} when {@code source} is {@code null}
     */
    @Override
    public Map<Object, Object> execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        ObjectAccessor accessor   = toObjectAccessor(source, context);
        MappingConfig  config     = context.config();
        Set<Object>    sourceKeys = new LinkedHashSet<>(accessor.keySet());

        // target should be Map<String, ?>
        Map<Object, Object> target = instantiate(config);

        // rule (sourceType -> Map.class)
        List<TypeMappingRule> mappingRules =
                context.mappingRegistry().find(source.getClass(), Map.class, context);

        for (TypeMappingRule mappingRule : mappingRules) {

            if (mappingRule != null && !mappingRule.mappings().isEmpty()) {
                // 1) Build entries from explicit bindings (rename/ignore/compute/constant/provider/reference)
                for (Map.Entry<String, PropertyMapping> entry : mappingRule.mappings().entrySet()) {
                    String          targetKey = entry.getKey();
                    PropertyMapping mapping   = entry.getValue();

                    Object value = applyValue(
                            accessor,
                            context,
                            mapping,
                            () -> safeGet(accessor, targetKey)
                    );

                    if (value == IgnoredValue.INSTANCE) {
                        markConsumedSourceKey(mapping, targetKey, sourceKeys);
                        continue;
                    }

                    Object mapped = materializeTree(value, context);
                    target.put(targetKey, mapped);
                    markConsumedSourceKey(mapping, targetKey, sourceKeys);
                }
            }
        }

        // rest mapping (unmapped properties)
        for (Object sourceKey : sourceKeys) {
            String key = coerceKey(sourceKey, config.mapKeyPolicy());

            if (key == null) {
                continue;
            }

            ObjectAccessor wrapped = accessor.get(sourceKey);
            target.put(key, materializeTree(wrapped.unwrap(), context));
        }

        return target;
    }

    /**
     * Mark a source key as consumed so it is not copied again during the "rest mapping" phase.
     *
     * <p>If the mapping is a simple {@link PropertyMapping.Reference} and its reference path is simple
     * (single segment), the referenced source key is removed from {@code sourceKeys}. In other cases,
     * the plan cannot reliably infer the consumed source key(s) and falls back to removing the current
     * {@code targetKey}.</p>
     *
     * @param mapping property mapping (may be {@code null})
     * @param targetKey target map key that was produced
     * @param sourceKeys mutable set of remaining source keys
     */
    private void markConsumedSourceKey(PropertyMapping mapping, String targetKey, Set<Object> sourceKeys) {
        if (mapping == null) {
            sourceKeys.remove(targetKey);
            return;
        }

        // If targetKey was filled by reading some source key directly -> remove that source key from "rest mapping"
        if (mapping instanceof PropertyMapping.Reference reference) {
            String sourcedReference = reference.sourceReference();
            if (sourcedReference != null) {
                PropertyPath propertyPath = PropertyPath.forPath(sourcedReference);
                if (propertyPath.isSimple()) {
                    sourceKeys.remove(propertyPath.path());
                    return;
                }
            }
        }

        // Otherwise we cannot reliably infer what source key(s) were consumed.
        // Keep sourceKeys as-is to avoid accidental data loss.
        // (Optional: remove targetKey if your source is a bean and names often match)
        sourceKeys.remove(targetKey);
    }

    /**
     * Coerce a source key into a {@link String} key according to {@link MapKeyPolicy}.
     *
     * @param key source key
     * @param policy key coercion policy
     * @return string key, or {@code null} when key should be skipped
     * @throws org.jmouse.core.mapping.errors.MappingException if policy is {@link MapKeyPolicy#FAIL} and key is not a string
     */
    private String coerceKey(Object key, MapKeyPolicy policy) {
        if (key == null) {
            return null;
        }

        if (key instanceof String string) {
            return string;
        }

        return switch (policy) {
            case STRINGIFY -> String.valueOf(key);
            case SKIP -> null;
            case FAIL -> throw toMappingException(
                    "map_key_not_string",
                    "Object-To-Map requires String keys, got: " + key.getClass().getName(),
                    null
            );
        };
    }

    /**
     * Materialize an arbitrary object into a tree-friendly representation.
     *
     * <p>The strategy is:</p>
     * <ul>
     *   <li>Scalar / simple values stay values</li>
     *   <li>Maps are expanded into {@code Map<String, Object>}</li>
     *   <li>Iterables / arrays are expanded into {@code List<Object>}</li>
     *   <li>If a value can be converted to {@code String}, it is treated as a leaf</li>
     *   <li>All other complex objects are expanded into a map tree</li>
     * </ul>
     *
     * <p>This method intentionally relies on existing {@link org.jmouse.core.convert.Conversion} rules
     * to classify leaf values instead of hardcoding type checks.</p>
     */
    private Object materializeTree(Object value, MappingContext context) {
        if (value == null) {
            return null;
        }

        ObjectAccessor accessor = toObjectAccessor(value, context);

        // 1) simple / scalar values are terminal (leaf)
        if (accessor.isSimple()) {
            return adaptValue(value, InferredType.forClass(Object.class), context);
        }

        // 2) map-like structures are expanded recursively
        if (accessor.isMap()) {
            return adaptValue(value, TREE_MAP_TYPE, context);
        }

        // 3) iterables and arrays are materialized as lists
        if (accessor.isIterable() || accessor.isArray()) {
            return adaptValue(value, TREE_LIST_TYPE, context);
        }

        // 4) values convertible to String are treated as leaves (e.g. URI, UUID, Date)
        if (hasConverterFor(accessor.getClassType(), String.class, context)) {
            return adaptValue(value, STRING_TYPE, context);
        }

        // 5) fallback: expand complex object into map tree
        return adaptValue(value, TREE_MAP_TYPE, context);
    }
}
