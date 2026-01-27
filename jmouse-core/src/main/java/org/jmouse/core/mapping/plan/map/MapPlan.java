package org.jmouse.core.mapping.plan.map;

import org.jmouse.core.MapFactory;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.support.AbstractMappingPlan;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MapPlan extends AbstractMappingPlan<Map<Object, Object>> implements MappingPlan<Map<Object, Object>> {

    private final Class<?> rawTarget;

    public MapPlan(InferredType targetType) {
        super(targetType);
        this.rawTarget = targetType.getRawType();
    }

    @Override
    public Map<Object, Object> execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        if (!(source instanceof Map<?, ?> mapSource)) {
            // allow fallback: try to wrap and unwrap? (keep strict for now)
            return null;
        }

        // example
        context.policy().nullHandlingPolicy(); ///

        InferredType mType = targetType.toMap();
        InferredType kType = mType.getFirst();
        InferredType vType = mType.getLast();

        Map<Object, Object> target = instantiate();

        for (Map.Entry<?, ?> entry : mapSource.entrySet()) {
            Object key   = adapt(entry.getKey(), kType, context);
            Object value = adapt(entry.getValue(), vType, context);
            target.put(key, value);
        }

        return target;
    }

    private Map<Object, Object> instantiate() {

        MapFactory.createMap(rawTarget);

        if (rawTarget.isInterface()) {
            return new LinkedHashMap<>();
        }
        try {
            @SuppressWarnings("unchecked")
            Constructor<? extends Map<Object, Object>> ctor =
                    (Constructor<? extends Map<Object, Object>>) rawTarget.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception ex) {
            // fallback to stable default
            return new LinkedHashMap<>();
        }
    }
}
