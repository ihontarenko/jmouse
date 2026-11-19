package org.jmouse.vm.graph;

/**
 * Supplies dependencies when a fact first becomes required. Each contributed rule must include that
 * fact among its prerequisites. Implementations use an immutable program/extension snapshot; they
 * must not depend on discovery order. Exceptions abort analysis without publishing a result.
 */
@FunctionalInterface
public interface DependencyProvider {

    Iterable<DependencyRule> dependencies(ProgramRequirement requirement);
}
