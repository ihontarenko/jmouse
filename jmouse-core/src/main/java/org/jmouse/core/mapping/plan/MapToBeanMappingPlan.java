package org.jmouse.core.mapping.plan.impl;

import org.jmouse.core.mapping.access.MapSourcePropertyReader;
import org.jmouse.core.mapping.factory.ObjectFactory;
import org.jmouse.core.mapping.plan.BeanPropertyOperation;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.runtime.MappingContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MapToBeanMappingPlan<T> implements MappingPlan<T> {

    private final ObjectFactory<T>               factory;
    private final List<BeanPropertyOperation<T>> operations;

    public MapToBeanMappingPlan(ObjectFactory<T> factory, List<BeanPropertyOperation<T>> operations) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.operations = Objects.requireNonNull(operations, "operations");
    }

    @Override
    public T execute(Object source, MappingContext context) {
        if (!(source instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("MapToBeanMappingPlan expects Map source");
        }

        T target = factory.create();
        MapSourcePropertyReader reader = new MapSourcePropertyReader(map);

        for (BeanPropertyOperation<T> op : operations) {
            op.apply(reader, target, context);
        }

        return target;
    }
}
