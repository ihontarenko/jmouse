package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.Optional;

/**
 * Selects a physical method for an already resolved invocation and receiver type. Empty means no
 * method was selected; ambiguity must be reported explicitly. Selection is separate from member
 * resolution, access validation, and checking whether the selected implementation is executable.
 */
@FunctionalInterface
public interface VirtualDispatchStrategy {

    Optional<ExecutableMethodKey> select(JavaTypeKey receiver, ExecutableMethodKey resolvedMethod);
}
