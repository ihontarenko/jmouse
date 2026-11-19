package org.jmouse.vm.graph.requirement;

import org.jmouse.java.model.symbol.JavaFieldKey;
import org.jmouse.vm.graph.ProgramRequirement;

import java.util.Objects;

public record FieldUsageRequirement(JavaFieldKey field) implements ProgramRequirement {

    public FieldUsageRequirement {
        Objects.requireNonNull(field, "field");
    }
}
