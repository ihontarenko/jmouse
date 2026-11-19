package org.jmouse.java.model.symbol;

import java.util.Objects;

public record JavaVariableKey(JavaSymbolKey owner, String name, int ordinal, JavaVariableKind kind)
        implements JavaSymbolKey {
    public JavaVariableKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        if (name.isEmpty() || ordinal < 0) {
            throw new IllegalArgumentException(
                    "A variable requires a name and a non-negative ordinal");
        }
    }

    @Override
    public String displayName() {
        return owner.displayName() + "#" + kind.name() + ":" + name + ":" + ordinal;
    }
}
