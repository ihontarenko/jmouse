package org.jmouse.vm.executable;

import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Objects;

/** Local identity is independent of its display name and a classfile's debug local table. */
public record ExecutableLocalId(ExecutableMethodKey owner, long ordinal) {

    public ExecutableLocalId {
        Objects.requireNonNull(owner, "owner");
        if (ordinal < 0) {
            throw new IllegalArgumentException("A local ordinal cannot be negative");
        }
    }
}
