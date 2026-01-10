package org.jmouse.core.context;

import org.jmouse.core.context.errors.ContextValueTypeMismatchException;
import org.jmouse.core.context.errors.MissingContextValueException;

import java.util.Map;

/**
 * Read-only key-value context contract.
 * Keys are typically {@link ContextKey} or {@link Class}, but any object may be used.
 */
public interface KeyValueContext {

    Map<Object, Object> asMapView();

    <T> T getValue(Object key);

    boolean containsKey(Object key);

    default <T> T getRequiredValue(Object key) {
        T value = getValue(key);

        if (value == null) {
            throw new MissingContextValueException(
                    "Required context value not found for key: " + key
            );
        }

        return value;
    }

    default <T> T getValue(ContextKey<T> key) {
        Object value = getValue((Object) key);

        if (value == null) {
            return null;
        }

        if (!key.type().isInstance(value)) {
            throw new ContextValueTypeMismatchException(
                    "Context value for key '" + key.name() + "' is not of required type '" + key.type().getName() + "'"
            );
        }

        return key.type().cast(value);
    }

    default <T> T getRequiredValue(ContextKey<T> key) {
        T value = getValue(key);

        if (value == null) {
            throw new MissingContextValueException(
                    "Required context value not found for key: " + key
            );
        }

        return value;
    }
}
