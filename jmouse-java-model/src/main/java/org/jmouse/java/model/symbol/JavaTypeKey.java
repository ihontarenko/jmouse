package org.jmouse.java.model.symbol;

import java.util.Objects;

public record JavaTypeKey(JavaModuleKey module, String binaryName) implements JavaSymbolKey {
    public JavaTypeKey {
        Objects.requireNonNull(module, "module");
        if (Objects.requireNonNull(binaryName, "binaryName").isBlank()) {
            throw new IllegalArgumentException("A type requires a binary name");
        }
    }

    @Override
    public String displayName() {
        return binaryName;
    }
}
