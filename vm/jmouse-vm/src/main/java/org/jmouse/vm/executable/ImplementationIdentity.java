package org.jmouse.vm.executable;

import java.util.Objects;

/** Explicit implementation identity/version, independent of registration or discovery order. */
public record ImplementationIdentity(String name, String version) {

    public ImplementationIdentity {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        if (name.isBlank() || version.isBlank()) {
            throw new IllegalArgumentException("An implementation requires a name and version");
        }
    }
}
