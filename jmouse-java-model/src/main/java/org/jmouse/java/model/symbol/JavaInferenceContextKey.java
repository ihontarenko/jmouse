package org.jmouse.java.model.symbol;

import java.util.Objects;

/** Snapshot-scoped owner for inferred capture variables that have no source declaration. */
public record JavaInferenceContextKey(String identity) implements JavaSymbolKey {
    public JavaInferenceContextKey {
        if (Objects.requireNonNull(identity, "identity").isBlank()) {
            throw new IllegalArgumentException("An inference context requires a snapshot identity");
        }
    }

    @Override
    public String displayName() {
        return identity;
    }
}
