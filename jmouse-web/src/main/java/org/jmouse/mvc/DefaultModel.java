package org.jmouse.mvc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link Model} using a LinkedHashMap internally.
 *
 * @author Ivan Hontarenko
 * @since 1.0
 */
public class DefaultModel implements Model {

    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override
    public Model addAttribute(String name, Object value) {
        Objects.requireNonNull(name, "Attribute name must not be null");
        attributes.putIfAbsent(name, value);
        return this;
    }

    @Override
    public Model setAttribute(String name, Object value) {
        Objects.requireNonNull(name, "Attribute name must not be null");
        attributes.put(name, value);
        return this;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Model removeAttribute(String name) {
        attributes.remove(name);
        return this;
    }

    @Override
    public boolean containsAttribute(String name) {
        return attributes.containsKey(name);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public int size() {
        return attributes.size();
    }

    @Override
    public String toString() {
        return "Model" + attributes;
    }

}
