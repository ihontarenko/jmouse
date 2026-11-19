package org.jmouse.java.model.symbol;

import java.util.Objects;

public record JavaPackageKey(JavaModuleKey module, String name) implements JavaSymbolKey {
    public JavaPackageKey {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(name, "name");
    }

    @Override
    public String displayName() {
        return name;
    }
}
