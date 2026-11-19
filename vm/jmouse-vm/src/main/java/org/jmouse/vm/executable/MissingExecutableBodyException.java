package org.jmouse.vm.executable;

import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Objects;

public final class MissingExecutableBodyException extends IllegalStateException {

    private final ExecutableMethodKey method;

    public MissingExecutableBodyException(ExecutableMethodKey method) {
        super("No executable body is bound for " + method);
        this.method = Objects.requireNonNull(method, "method");
    }

    public ExecutableMethodKey method() {
        return method;
    }
}
