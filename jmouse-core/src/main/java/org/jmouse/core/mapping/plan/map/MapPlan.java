package org.jmouse.core.mapping.plan.map;

import org.jmouse.core.MapFactory;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.config.NullHandlingPolicy;
import org.jmouse.core.mapping.config.TypeMismatchPolicy;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.support.AbstractPlan;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.Map;

public final class MapPlan extends AbstractPlan<Map<Object, Object>> implements MappingPlan<Map<Object, Object>> {

    public MapPlan(InferredType targetType) {
        super(targetType);
    }

    @Override
    public Map<Object, Object> execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        if (!(source instanceof Map<?, ?> mapSource)) {
            return null;
        }

        MappingPolicy policy = context.policy();

        InferredType mType = getTargetType().toMap();
        InferredType kType = mType.getFirst();
        InferredType vType = mType.getLast();

        Map<Object, Object> target = instantiate(context.config());

        ObjectAccessor accessor = toObjectAccessor(mapSource, context);

        for (Map.Entry<?, ?> entry : mapSource.entrySet()) {
            Object key   = entry.getKey();
            Object value = entry.getValue();

            if (value == null && policy.nullHandlingPolicy() == NullHandlingPolicy.SKIP) {
                continue;
            }

            try {
                key = adaptValue(key, kType, context);
                value = adaptValue(value, vType, context);
            } catch (RuntimeException exception) {
                if (policy.typeMismatchPolicy() == TypeMismatchPolicy.FAIL) {
                    throw toMappingException(
                            "map_entry_adapt_failed",
                            "Failed to adapt map entry key/value to target map types", exception
                    );
                }
                continue;
            }

            if (value == null && policy.nullHandlingPolicy() == NullHandlingPolicy.SKIP) {
                continue;
            }

            target.put(key, value);
        }

        return target;
    }

    private Map<Object, Object> instantiate(MappingConfig config) {
        return config.mapFactory().get();
    }
}
