package org.jmouse.vm.graph.requirement;

import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.graph.ProgramRequirement;

import java.util.Objects;

public record ClassInitializationRequirement(JavaTypeKey type) implements ProgramRequirement {

    public ClassInitializationRequirement {
        Objects.requireNonNull(type, "type");
    }
}
