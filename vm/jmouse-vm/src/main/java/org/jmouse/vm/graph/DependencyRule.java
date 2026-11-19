package org.jmouse.vm.graph;

import java.util.Objects;
import java.util.Set;

/** The consequence becomes required only when every prerequisite is required. */
public record DependencyRule(
        Set<ProgramRequirement> prerequisites,
        ProgramRequirement required,
        DependencyReason reason) {

    public DependencyRule {
        prerequisites = Set.copyOf(prerequisites);
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(reason, "reason");
    }
}
