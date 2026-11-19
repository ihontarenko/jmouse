package org.jmouse.vm.graph.requirement;

import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Objects;

public record VirtualInvocationRequirement(ExecutableMethodKey resolvedMethod)
        implements ProgramRequirement {

    public VirtualInvocationRequirement {
        Objects.requireNonNull(resolvedMethod, "resolvedMethod");
    }
}
