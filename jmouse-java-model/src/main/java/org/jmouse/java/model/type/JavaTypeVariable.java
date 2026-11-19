package org.jmouse.java.model.type;

import org.jmouse.java.model.symbol.JavaTypeVariableKey;

import java.util.Objects;

public record JavaTypeVariable(JavaTypeVariableKey identity) implements JavaType {
    public JavaTypeVariable {
        Objects.requireNonNull(identity, "identity");
    }

    @Override
    public String displayName() {
        return identity.name();
    }
}
