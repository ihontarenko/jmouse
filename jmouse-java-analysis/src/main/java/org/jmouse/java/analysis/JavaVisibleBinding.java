package org.jmouse.java.analysis;

import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.type.JavaType;

import java.util.Objects;
import java.util.Optional;

/**
 * A visible declaration. Native scope copies without source identity are explicit through the empty
 * key.
 */
public record JavaVisibleBinding(String name, JavaType type, Optional<JavaSymbolKey> identity) {
    public JavaVisibleBinding {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(identity, "identity");
    }
}
