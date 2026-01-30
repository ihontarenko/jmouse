package org.jmouse.core.mapping.plan.map;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.config.TypeMismatchPolicy;
import org.jmouse.core.mapping.plan.support.AbstractMapPlan;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.Map;

public final class MapToMapPlan extends AbstractMapPlan<Map<Object, Object>> {

    private static final InferredType NESTED_MAP_TYPE = InferredType.forParametrizedClass(
            Map.class, Object.class, Object.class
    );

    public MapToMapPlan(TypedValue<Map<Object, Object>> typedValue) {
        super(typedValue);
    }

    @Override
    public Map<Object, Object> execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        MappingPolicy policy = context.policy();

        InferredType mapType   = getTargetType().toMap();
        InferredType valueType = mapType.getLast();
        InferredType keyType   = mapType.getFirst();

        Map<Object, Object> target   = instantiate(context.config());
        ObjectAccessor      accessor = toObjectAccessor(source, context);

        if (!accessor.isMap()) {
            return null;
        }

        for (Object keyValue : accessor.keySet()) {
            ObjectAccessor wrapped  = accessor.get(keyValue);
            Object         mapValue = wrapped.unwrap();

            InferredType effectiveType = valueType;

            if (effectiveType.isObject() && !wrapped.isSimple()) {
                effectiveType = NESTED_MAP_TYPE;
            }

            try {
                keyValue = adaptValue(keyValue, keyType, context);
                mapValue = adaptValue(mapValue, effectiveType, context);
            } catch (RuntimeException exception) {
                if (policy.typeMismatchPolicy() == TypeMismatchPolicy.FAIL) {
                    throw toMappingException(
                            "map_entry_adapt_failed",
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
