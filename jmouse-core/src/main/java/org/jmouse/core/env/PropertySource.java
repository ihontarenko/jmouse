package org.jmouse.core.env;

import java.util.Map;

/**
 * 📦 Property source abstraction.
 * Used to resolve config values (e.g. from file, env, etc).
 *
 * @param <T> type of the raw underlying source
 */
public interface PropertySource<T> {

    /**
     * 🏷️ Name of this source.
     *
     * @return source name
     */
    String getName();

    /**
     * 🔍 Raw source object.
     *
     * @return source
     */
    T getSource();

    /**
     * ❓ Check if a key exists.
     *
     * @param name property name
     * @return {@code true} if defined
     */
    default boolean containsProperty(String name) {
        return getProperty(name) != null;
    }

    /**
     * 📥 Get property by key.
     *
     * @param name property name
     * @return value or {@code null}
     */
    Object getProperty(String name);

    /**
     * 📚 All keys defined in this source.
     *
     * @return array of keys
     */
    String[] getPropertyNames();

    /**
     * 🧾 Load from classpath resource.
     *
     * @param path location (e.g. "/config.yml")
     * @return map-based source
     */
    static PropertySource<Map<String, Object>> loadProperties(String path) {
        return new ClasspathPropertySource(path, path);
    }

    /**
     * 🧾 Load from classpath using simple name.
     *
     * @param type class to derive resource name from
     * @return map-based source
     */
    static PropertySource<Map<String, Object>> loadProperties(Class<?> type) {
        return loadProperties(type.getSimpleName() + ".properties");
    }
}
