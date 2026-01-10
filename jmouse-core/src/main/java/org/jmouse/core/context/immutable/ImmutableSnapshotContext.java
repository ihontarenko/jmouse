package org.jmouse.core.context.immutable;

import org.jmouse.core.context.KeyValueContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 🧊 Immutable snapshot of a key-value context.
 *
 * <p>
 * Represents a <b>defensive copy</b> of context values
 * captured at a specific point in time.
 * </p>
 *
 * <p>
 * Safe for sharing across threads and execution stages.
 * </p>
 */
public final class ImmutableSnapshotContext implements KeyValueContext {

    /**
     * 🔒 Immutable value storage.
     */
    private final Map<Object, Object> values;

    /**
     * 📸 Capture immutable snapshot.
     *
     * @param values source values
     */
    public ImmutableSnapshotContext(Map<Object, Object> values) {
        this.values = Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNull(values, "values"))
        );
    }

    /**
     * 👀 Read-only map view.
     *
     * @return immutable map view
     */
    @Override
    public Map<Object, Object> asMapView() {
        return values;
    }

    /**
     * 🔍 Get value by key.
     *
     * @param key lookup key
     * @param <T> expected type
     * @return value or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(Object key) {
        return (T) values.get(key);
    }

    /**
     * ❓ Check key presence.
     *
     * @param key lookup key
     * @return {@code true} if key exists
     */
    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    /**
     * 🧾 Debug-friendly representation.
     */
    @Override
    public String toString() {
        return "ImmutableContextValues" + values;
    }
}
