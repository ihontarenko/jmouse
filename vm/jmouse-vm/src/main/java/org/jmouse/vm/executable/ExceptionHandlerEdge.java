package org.jmouse.vm.executable;

import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.Objects;
import java.util.Optional;

/** Empty exception type means catch-all; handlers are tested in their declared list order. */
public record ExceptionHandlerEdge(Optional<JavaTypeKey> exceptionType, BasicBlockId target) {

    public ExceptionHandlerEdge {
        Objects.requireNonNull(exceptionType, "exceptionType");
        Objects.requireNonNull(target, "target");
    }
}
