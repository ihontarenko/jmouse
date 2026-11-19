package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.Set;

public final class VirtualDispatchException extends IllegalStateException {

    private final JavaTypeKey receiver;
    private final ExecutableMethodKey resolvedMethod;
    private final Set<ExecutableMethodKey> candidates;

    public VirtualDispatchException(
            JavaTypeKey receiver,
            ExecutableMethodKey resolvedMethod,
            Set<ExecutableMethodKey> candidates) {
        super(
                "Ambiguous virtual dispatch for "
                        + receiver
                        + " and "
                        + resolvedMethod
                        + ": "
                        + candidates);
        this.receiver = receiver;
        this.resolvedMethod = resolvedMethod;
        this.candidates = Set.copyOf(candidates);
    }

    public JavaTypeKey receiver() {
        return receiver;
    }

    public ExecutableMethodKey resolvedMethod() {
        return resolvedMethod;
    }

    public Set<ExecutableMethodKey> candidates() {
        return candidates;
    }
}
