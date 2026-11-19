package org.jmouse.vm.graph;

import org.jmouse.vm.program.ExecutableMethodKey;

/**
 * Frontend or substitution dependencies for a physical implementation. Rules must be anchored to
 * its MethodImplementationRequirement. This describes closure, not proof of executable-body
 * validity.
 */
@FunctionalInterface
public interface MethodDependencyProvider {

    Iterable<DependencyRule> dependencies(ExecutableMethodKey method);
}
