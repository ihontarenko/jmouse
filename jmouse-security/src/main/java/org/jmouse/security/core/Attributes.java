package org.jmouse.security.core;

import java.util.Map;

/**
 * Arbitrary key/value context (environment, device, geo, risk, ip...).
 */
public interface Attributes {

    static Attributes mutable() {
        return new MutableAttributes();
    }

    Object get(String key);

    void put(String key, Object value);

    Map<String, Object> asMap();

}