package org.jmouse.vm.graph.requirement;

import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.graph.ProgramRequirement;

import java.util.Objects;

public record TypeInstantiationRequirement(JavaTypeKey type) implements ProgramRequirement {

    public TypeInstantiationRequirement {
        Objects.requireNonNull(type, "type");
    }
}
