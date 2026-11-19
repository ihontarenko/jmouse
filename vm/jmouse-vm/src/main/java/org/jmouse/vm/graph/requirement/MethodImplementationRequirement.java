package org.jmouse.vm.graph.requirement;

import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Objects;

public record MethodImplementationRequirement(ExecutableMethodKey method)
        implements ProgramRequirement {

    public MethodImplementationRequirement {
        Objects.requireNonNull(method, "method");
    }
}
