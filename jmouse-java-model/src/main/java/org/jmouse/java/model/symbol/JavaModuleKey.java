package org.jmouse.java.model.symbol;

import java.util.Objects;

public record JavaModuleKey(String name) implements JavaSymbolKey {
    public JavaModuleKey {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public String displayName() {
        return name;
    }
}
