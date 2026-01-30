package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.config.MappingConfig;

import java.util.Map;

abstract public class AbstractMapPlan<T extends Map<?, ?>> extends AbstractPlan<T> {

    protected AbstractMapPlan(TypedValue<T> typedValue) {
        super(typedValue);
    }

    protected Map<Object, Object> instantiate(MappingConfig config) {
        getTypedValue().getValue().get();
        return config.mapFactory().get();
    }

}
