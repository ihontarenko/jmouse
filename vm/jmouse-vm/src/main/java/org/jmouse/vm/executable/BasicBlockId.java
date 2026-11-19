package org.jmouse.vm.executable;

import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Objects;

public record BasicBlockId(ExecutableMethodKey owner, long ordinal) {

    public BasicBlockId {
        Objects.requireNonNull(owner, "owner");
        if (ordinal < 0) {
            throw new IllegalArgumentException("A block ordinal cannot be negative");
        }
    }
}
