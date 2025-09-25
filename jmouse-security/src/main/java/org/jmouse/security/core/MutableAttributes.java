package org.jmouse.security.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📝 Thread-safe implementation of {@link Attributes}.
 *
 * <p>Backed by a {@link ConcurrentHashMap}.</p>
 */
public final class MutableAttributes implements Attributes {

    private final Map<Object, Object> attributes;

    public MutableAttributes() {
        attributes = new ConcurrentHashMap<>();
    }

    /**
     * 🔍 Lookup a value by key.
     *
     * <pre>{@code
     * MutableAttributes attributes = new MutableAttributes();
     * attributes.set("ip", "127.0.0.1");
     * System.out.println(attributes.get("ip")); // 127.0.0.1
     * }</pre>
     *
     * @param key attribute key
     * @return value or {@code null}
     */
    @Override
    public Object get(Object key) {
        return attributes.get(key);
    }

    /**
     * ➕ Add or replace a key/value pair.
     *
     * <pre>{@code
     * MutableAttributes attributes = new MutableAttributes();
     * attributes.set("device", "laptop");
     * }</pre>
     *
     * @param key   attribute key
     * @param value attribute value
     */
    @Override
    public void set(Object key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 📦 Bulk insert attributes.
     *
     * <pre>{@code
     * MutableAttributes attributes = new MutableAttributes();
     * attributes.batch(Map.of("country", "UA", "riskScore", 42));
     * }</pre>
     *
     * @param values map of attributes
     */
    @Override
    public void batch(Map<?, Object> values) {
        attributes.putAll(values);
    }

    /**
     * ❌ Remove a key/value pair.
     *
     * <pre>{@code
     * MutableAttributes attributes = new MutableAttributes();
     * attributes.set("session", "abc");
     * attributes.remove("session");
     * }</pre>
     *
     * @param key attribute key
     */
    @Override
    public void remove(Object key) {
        attributes.remove(key);
    }

    /**
     * 🧹 Clear all attributes.
     *
     * <pre>{@code
     * MutableAttributes attributes = new MutableAttributes();
     * attributes.set("x", 1);
     * attributes.clear();
     * System.out.println(attributes.asMap().isEmpty()); // true
     * }</pre>
     */
    @Override
    public void clear() {
        attributes.clear();
    }

    /**
     * 📜 Expose attributes as a map view.
     *
     * <pre>{@code
     * MutableAttributes attributes = new MutableAttributes();
     * attributes.set("ip", "127.0.0.1");
     * System.out.println(attributes.asMap()); // {ip=127.0.0.1}
     * }</pre>
     *
     * @return underlying map
     */
    @Override
    public Map<Object, Object> asMap() {
        return attributes;
    }
}
