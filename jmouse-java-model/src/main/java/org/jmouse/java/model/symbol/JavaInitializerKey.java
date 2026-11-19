package org.jmouse.java.model.symbol;

import java.util.Objects;

public record JavaInitializerKey(JavaTypeKey owner, int ordinal, Kind kind)
        implements JavaSymbolKey {
    public enum Kind {
        STATIC,
        INSTANCE
    }

    public JavaInitializerKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(kind, "kind");
    }

    @Override
    public String displayName() {
        return owner.displayName() + "#" + kind.name() + ":" + ordinal;
    }
}
