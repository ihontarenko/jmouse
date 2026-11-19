package org.jmouse.vm.graph;

import java.util.Objects;

public record EntryPoint(ProgramRequirement requirement, DependencyReason reason) {

    public EntryPoint {
        Objects.requireNonNull(requirement, "requirement");
        Objects.requireNonNull(reason, "reason");
    }
}
