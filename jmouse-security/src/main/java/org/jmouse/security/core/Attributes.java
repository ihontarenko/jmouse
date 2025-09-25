package org.jmouse.security.core;

import java.util.Map;

/**
 * 🧾 Arbitrary key/value context store.
 *
 * <p>Used for environment, device, geo, risk, IP, and other
 * contextual attributes in security evaluation.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Attributes attrs = Attributes.mutable();
 * attrs.set("ip", "192.168.0.1");
 * attrs.set("device", "laptop");
 *
 * System.out.println(attrs.get("ip"));       // 192.168.0.1
 * System.out.println(attrs.asMap().size());  // 2
 *
 * attrs.remove("device");
 * attrs.clear();
 * }</pre>
 */
public interface Attributes {

    /**
     * 🛠️ Create a new mutable attribute bag.
     *
     * @return fresh {@link Attributes} instance
     */
    static Attributes mutable() {
        return new MutableAttributes();
    }

    /**
     * 🔍 Lookup a value by key.
     *
     * @param key attribute key
     * @return value or {@code null}
     */
    Object get(Object key);

    /**
     * ➕ Add or replace a key/value pair.
     *
     * @param key   attribute key
     * @param value attribute value
     */
    void set(Object key, Object value);

    /**
     * 📦 Bulk insert attributes.
     *
     * @param values map of attributes
     */
    void batch(Map<?, Object> values);

    /**
     * ❌ Remove a key/value pair.
     *
     * @param key attribute key
     */
    void remove(Object key);

    /**
     * 🧹 Clear all attributes.
     */
    void clear();

    /**
     * 📜 Expose attributes as a map view.
     *
     * @return underlying map
     */
    Map<Object, Object> asMap();
}
