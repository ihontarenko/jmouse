package org.jmouse.vm.graph;

import java.util.Objects;

public final class ProgramDependencyException extends IllegalStateException {

    private final ProgramRequirement requirement;

    public ProgramDependencyException(ProgramRequirement requirement, String detail) {
        super(detail + ": " + requirement);
        this.requirement = Objects.requireNonNull(requirement, "requirement");
    }

    public ProgramRequirement requirement() {
        return requirement;
    }
}
