package org.jmouse.vm.executable;

/** Dispatch of an already resolved physical executable reference. */
public enum ExecutableInvocationKind {
    STATIC,
    SPECIAL,
    VIRTUAL,
    INTERFACE
}
