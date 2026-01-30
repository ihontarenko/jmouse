package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.config.MappingConfig;

import java.util.Map;
import java.util.function.Supplier;

abstract public class AbstractMapPlan<T extends Map<?, ?>> extends AbstractPlan<T> {

    protected AbstractMapPlan(TypedValue<T> typedValue) {
        super(typedValue);
    }

    @SuppressWarnings("unchecked")
    protected Map<Object, Object> instantiate(MappingConfig config) {
        Map<Object, Object> instance = (Map<Object, Object>) getTypedValue().getValue().get();
        Supplier<T>         factory  = (Supplier<T>) config.mapFactory();

        if (instance == null) {
            instance = (Map<Object, Object>) factory.get();
        }

        return Verify.nonNull(instance, "instance");
    }

}
