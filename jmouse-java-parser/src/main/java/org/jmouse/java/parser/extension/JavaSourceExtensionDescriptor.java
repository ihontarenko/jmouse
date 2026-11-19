package org.jmouse.java.parser.extension;

import java.util.Objects;
import java.util.Set;
import org.jmouse.java.model.version.JavaFeatureId;

/** Explicit dependency and feature metadata; classpath discovery never chooses an override order. */
public record JavaSourceExtensionDescriptor(String identity, String revision, Set<String> requires,
                                             Set<JavaFeatureId> features) {
    public JavaSourceExtensionDescriptor {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(revision, "revision");
        requires = Set.copyOf(requires);
        features = Set.copyOf(features);
        if (identity.isBlank() || !identity.equals(identity.strip()) || identity.indexOf('.') < 1
                || revision.isBlank()) {
            throw new IllegalArgumentException("An extension needs a qualified identity and a nonblank revision");
        }
        if (requires.contains(identity)) {
            throw new IllegalArgumentException("An extension cannot require itself");
        }
    }
}
