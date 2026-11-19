package org.jmouse.java.model.version;

import java.util.Objects;

/** Open, namespaced feature identity; source features are distinct from JVM-format features. */
public record JavaFeatureId(String name) {
    public JavaFeatureId {
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || !name.equals(name.strip()) || name.indexOf('.') < 1 || name.endsWith(".")) {
            throw new IllegalArgumentException("A Java feature needs a qualified name: " + name);
        }
    }
}
