package org.jmouse.vm.graph.requirement;

import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Objects;

/** Method selection for one instantiated receiver and an already resolved invocation. */
public record VirtualDispatchRequirement(ExecutableMethodKey resolvedMethod, JavaTypeKey receiver)
        implements ProgramRequirement {

    public VirtualDispatchRequirement {
        Objects.requireNonNull(resolvedMethod, "resolvedMethod");
        Objects.requireNonNull(receiver, "receiver");
    }
}
