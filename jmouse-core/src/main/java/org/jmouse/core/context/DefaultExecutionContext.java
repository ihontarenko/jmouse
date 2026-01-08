package org.jmouse.core.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;

public final class DefaultExecutionContext implements ExecutionContext {

    private final Map<ContextKey<?>, Object> entries;

    public DefaultExecutionContext() {
        this.entries = Collections.emptyMap();
    }

    private DefaultExecutionContext(Map<ContextKey<?>, Object> entries) {
        this.entries = Collections.unmodifiableMap(entries);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ContextKey<T> key) {
        nonNull(key, "key");
        Object value = entries.get(key);
        return value == null ? null : (T) value;
    }

    @Override
    public <T> ExecutionContext with(ContextKey<T> key, T value) {
        nonNull(key, "key");

        Map<ContextKey<?>, Object> copy = new LinkedHashMap<>(entries);

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return new DefaultExecutionContext(copy);
    }

    @Override
    public Map<ContextKey<?>, Object> entries() {
        return entries;
    }

    @Override
    public ExecutionSnapshot snapshot() {
        return new ExecutionSnapshot(entries);
    }
}
