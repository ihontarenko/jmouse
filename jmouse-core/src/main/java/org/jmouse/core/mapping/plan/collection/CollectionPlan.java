package org.jmouse.core.mapping.plan.collection;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.support.AbstractPlan;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.*;

public final class CollectionPlan extends AbstractPlan<Collection<Object>>
        implements MappingPlan<Collection<Object>> {

    private final Class<?> rawTarget;

    public CollectionPlan(InferredType targetType) {
        super(targetType);
        this.rawTarget = targetType.getRawType();
    }

    @Override
    public Collection<Object> execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        Iterable<?> iterable = asIterable(source);
        if (iterable == null) {
            return null;
        }

        InferredType       elementType = targetType.toCollection().getFirst();
        Collection<Object> target      = instantiate();

        for (Object item : iterable) {
            target.add(adaptValue(item, elementType, context));
        }

        return target;
    }

    private Iterable<?> asIterable(Object source) {
        if (source instanceof Iterable<?> i) {
            return i;
        }

        if (source.getClass().isArray()) {
            int          length     = Array.getLength(source);
            List<Object> collection = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                collection.add(Array.get(source, i));
            }

            return collection;
        }

        return null;
    }

    private Collection<Object> instantiate() {
        if (rawTarget.isInterface()) {
            if (Set.class.isAssignableFrom(rawTarget)) {
                return new LinkedHashSet<>();
            }
            return new ArrayList<>();
        }

        try {
            @SuppressWarnings("unchecked")
            Constructor<? extends Collection<Object>> ctor =
                    (Constructor<? extends Collection<Object>>) rawTarget.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception ex) {
            // stable fallback
            if (Set.class.isAssignableFrom(rawTarget)) {
                return new LinkedHashSet<>();
            }
            return new ArrayList<>();
        }
    }
}
