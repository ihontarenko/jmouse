package org.jmouse.vm.graph;

import java.util.Collection;

/**
 * Supplies dependencies when a fact first becomes required. Each contributed rule must include that
 * fact among its prerequisites. Implementations use an immutable program/extension snapshot; they
 * must not depend on discovery order. Exceptions abort analysis without publishing a result.
 */
@FunctionalInterface
public interface DependencyProvider {

    Collection<DependencyRule> dependencies(ProgramRequirement requirement);
}
