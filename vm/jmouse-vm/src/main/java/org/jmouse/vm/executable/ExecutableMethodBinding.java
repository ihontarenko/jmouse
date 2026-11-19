package org.jmouse.vm.executable;

import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Objects;

public record ExecutableMethodBinding(ImplementationIdentity identity, ExecutableMethodBody body) {

    public ExecutableMethodBinding {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(body, "body");
    }

    public ExecutableMethodKey method() {
        return body.owner();
    }
}
