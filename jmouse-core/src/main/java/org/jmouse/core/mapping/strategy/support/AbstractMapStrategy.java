package org.jmouse.core.mapping.strategy.support;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.reflection.InferredType;

import java.util.Map;
import java.util.function.Supplier;

abstract public class AbstractMapStrategy<T extends Map<?, ?>> extends AbstractStrategy<T> {

    @SuppressWarnings("unchecked")
    protected Map<Object, Object> instantiate(MappingConfig config, TypedValue<T> typedValue) {
        Map<Object, Object> instance = (Map<Object, Object>) typedValue.getValue().get();
        Supplier<T>         factory  = (Supplier<T>) config.mapFactory();

        if (instance == null) {
            instance = (Map<Object, Object>) factory.get();
        }

        return Verify.nonNull(instance, "instance");
    }

    public TypedValue<?> getMapTypedValue(Map<Object, Object> target, Object key, InferredType type) {
        TypedValue<Object> typedValue = TypedValue.of(type);

        if (target.containsKey(key)) {
            Object value = target.get(key);
            typedValue = TypedValue.of(InferredType.forInstance(value));
            typedValue = typedValue.withInstance(value);
        }

        return typedValue;
    }

}
