package org.jmouse.security.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MutableAttributes implements Attributes {

    private final Map<String, Object> attributes;

    public MutableAttributes() {
        attributes = new ConcurrentHashMap<>();
    }

    @Override
    public Object get(String key) {
        return attributes.get(key);
    }

    @Override
    public void put(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> asMap() {
        return attributes;
    }

}
