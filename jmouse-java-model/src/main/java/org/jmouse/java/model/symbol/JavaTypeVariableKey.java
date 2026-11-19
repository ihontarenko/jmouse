package org.jmouse.java.model.symbol;

import java.util.Objects;

public record JavaTypeVariableKey(JavaSymbolKey owner, String name, int ordinal)
        implements JavaSymbolKey {
    public JavaTypeVariableKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        if (name.isEmpty() || ordinal < 0) {
            throw new IllegalArgumentException(
                    "A type variable requires a name and a non-negative ordinal");
        }
    }

    @Override
    public String displayName() {
        return owner.displayName() + "<" + name + ":" + ordinal + ">";
    }
}
