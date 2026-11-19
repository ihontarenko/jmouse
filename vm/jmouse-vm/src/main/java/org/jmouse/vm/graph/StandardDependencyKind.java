package org.jmouse.vm.graph;

public enum StandardDependencyKind implements DependencyKind {
    ENTRY_POINT,
    DECLARATION,
    SIGNATURE,
    CALL,
    FIELD_ACCESS,
    ALLOCATION,
    VIRTUAL_DISPATCH,
    CLASS_INITIALIZATION,
    SUBSTITUTION,
    RUNTIME,
    RESOURCE,
    REFLECTION,
    SERVICE
}
