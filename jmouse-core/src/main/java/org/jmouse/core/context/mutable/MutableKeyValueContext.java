package org.jmouse.core.context.mutable;

import org.jmouse.core.Verify;
import org.jmouse.core.context.ContextKey;
import org.jmouse.core.context.KeyValueContext;

import java.util.Map;

public interface MutableKeyValueContext extends KeyValueContext {

    void setValue(Object key, Object value);

    void removeValue(Object key);

    void clear();

    default void copyValuesFrom(KeyValueContext source) {
        for (Map.Entry<Object, Object> entry : Verify.nonNull(source, "source").asMapView().entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
    }

    default void copyValuesTo(MutableKeyValueContext target) {
        Verify.nonNull(target, "target");
        target.copyValuesFrom(this);
    }

    default void setValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("value must be non-null");
        }
        setValue(value.getClass(), value);
    }

    default <T> void setValue(ContextKey<T> key, T value) {
        setValue((Object) key, value);
    }
}
