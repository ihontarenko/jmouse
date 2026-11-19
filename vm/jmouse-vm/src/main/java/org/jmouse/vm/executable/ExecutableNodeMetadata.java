package org.jmouse.vm.executable;

import org.jmouse.vm.program.ProgramOrigin;

import java.util.List;
import java.util.Objects;

public record ExecutableNodeMetadata(ExecutableNodeId identity, List<ProgramOrigin> origins) {

    public ExecutableNodeMetadata {
        Objects.requireNonNull(identity, "identity");
        origins = List.copyOf(origins);
        if (origins.isEmpty()) {
            throw new IllegalArgumentException(
                    "An executable node requires input or binding provenance");
        }
    }
}
