package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.reflection.InferredType;

import java.util.Map;

abstract public class AbstractMapPlan<T extends Map<?, ?>> extends AbstractPlan<T> {

    protected AbstractMapPlan(InferredType targetType) {
        super(targetType);
    }

    protected Map<Object, Object> instantiate(MappingConfig config) {
        return config.mapFactory().get();
    }

}
