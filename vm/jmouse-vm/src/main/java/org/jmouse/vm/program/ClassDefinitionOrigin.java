package org.jmouse.vm.program;

import java.util.Objects;

public record ClassDefinitionOrigin(ProgramInput input, ProgramOrigin location) {

    public ClassDefinitionOrigin {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(location, "location");
    }
}
