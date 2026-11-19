package org.jmouse.vm.executable;

import org.jmouse.java.model.type.JavaVoidType;
import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Objects;

/**
 * Resolved close call. Null guards, reverse closing and suppression remain lowering obligations.
 */
public record ResourceCloseBinding(
        ExecutableMethodKey method, ExecutableInvocationKind invocation) {

    public ResourceCloseBinding {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(invocation, "invocation");
        if ((invocation != ExecutableInvocationKind.VIRTUAL
                        && invocation != ExecutableInvocationKind.INTERFACE)
                || !method.descriptor().parameters().isEmpty()
                || method.descriptor().result() != JavaVoidType.VOID) {
            throw new IllegalArgumentException(
                    "Resource closing requires a resolved no-argument void instance call");
        }
    }
}
