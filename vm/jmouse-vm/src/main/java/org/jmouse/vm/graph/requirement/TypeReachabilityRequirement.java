package org.jmouse.vm.graph.requirement;

import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.graph.ProgramRequirement;

import java.util.Objects;

public record TypeReachabilityRequirement(JavaTypeKey type) implements ProgramRequirement {

    public TypeReachabilityRequirement {
        Objects.requireNonNull(type, "type");
    }
}
