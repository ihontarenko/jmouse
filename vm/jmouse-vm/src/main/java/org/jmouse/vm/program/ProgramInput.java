package org.jmouse.vm.program;

import java.util.Objects;

/** Stable identity of an input snapshot; the provider supplies its content fingerprint. */
public record ProgramInput(ProgramInputKind kind, String name, String fingerprint) {

    public ProgramInput {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (name.isBlank() || fingerprint.isBlank()) {
            throw new IllegalArgumentException("A program input requires a name and fingerprint");
        }
    }
}
