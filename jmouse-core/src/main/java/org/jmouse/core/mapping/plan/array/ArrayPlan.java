package org.jmouse.core.mapping.plan.array;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.support.AbstractPlan;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public final class ArrayPlan<T> extends AbstractPlan<T> implements MappingPlan<T> {

    private final Class<?> rawArrayType;

    public ArrayPlan(InferredType targetType) {
        super(targetType);
        this.rawArrayType = targetType.getRawType();
    }

    @Override
    public T execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        List<Object> items = toList(source);
        if (items == null) {
            return null;
        }

        InferredType componentType = targetType.getComponentType();
        Class<?> componentRaw = componentType.getRawType();

        Object array = Array.newInstance(componentRaw, items.size());

        for (int i = 0; i < items.size(); i++) {
            Object adapted = adaptValue(items.get(i), componentType, context);
            Array.set(array, i, adapted);
        }

        @SuppressWarnings("unchecked")
        T result = (T) array;
        return result;
    }

    private List<Object> toList(Object source) {
        if (source instanceof Iterable<?> it) {
            List<Object> list = new ArrayList<>();
            for (Object o : it) list.add(o);
            return list;
        }
        if (source.getClass().isArray()) {
            int len = Array.getLength(source);
            List<Object> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) list.add(Array.get(source, i));
            return list;
        }
        return null;
    }
}
