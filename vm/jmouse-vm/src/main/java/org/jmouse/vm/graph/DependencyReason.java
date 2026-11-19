package org.jmouse.vm.graph;

import org.jmouse.vm.program.ProgramOrigin;

import java.util.List;
import java.util.Objects;

public record DependencyReason(
        DependencyKind kind, String description, List<ProgramOrigin> origins) {

    public DependencyReason {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(description, "description");
        origins = List.copyOf(origins);
        if (description.isBlank()) {
            throw new IllegalArgumentException("A dependency requires an explanation");
        }
    }
}
