package org.jmouse.vm.graph.requirement;

import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.runtime.RuntimeComponentId;

import java.util.Objects;

public record RuntimeComponentRequirement(RuntimeComponentId component) implements ProgramRequirement {

    public RuntimeComponentRequirement {
        Objects.requireNonNull(component, "component");
    }
}
