package org.jmouse.java.model.type;

import java.util.Locale;
import java.util.Objects;

public record JavaWildcardType(Bound kind, JavaType bound) implements JavaType {
    public enum Bound {
        UNBOUNDED,
        EXTENDS,
        SUPER
    }

    public JavaWildcardType {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(bound, "bound");
        if (kind == Bound.UNBOUNDED && bound != JavaSpecialType.NONE
                || kind != Bound.UNBOUNDED
                        && (bound instanceof JavaPrimitiveType
                                || bound instanceof JavaSpecialType
                                || bound == JavaVoidType.VOID)) {
            throw new IllegalArgumentException(
                    "A wildcard requires a reference bound or an unbounded marker");
        }
    }

    @Override
    public String displayName() {
        return kind == Bound.UNBOUNDED
                ? "?"
                : "? " + kind.name().toLowerCase(Locale.ROOT) + " " + bound.displayName();
    }
}
