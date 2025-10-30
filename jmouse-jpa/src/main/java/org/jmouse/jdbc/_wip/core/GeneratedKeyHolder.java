package org.jmouse.jdbc._wip.core;

import java.util.Optional;

/**
 * 🗝️ Simple in-memory KeyHolder.
 */
public final class GeneratedKeyHolder implements KeyHolder {

    private Object key;

    @Override
    public Optional<Object> getKey() {
        return Optional.ofNullable(key);
    }

    @Override
    public void setKey(Object key) {
        this.key = key;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getKeyAs(Class<T> type) {
        return key == null ? Optional.empty() : Optional.of((T) key);
    }

}