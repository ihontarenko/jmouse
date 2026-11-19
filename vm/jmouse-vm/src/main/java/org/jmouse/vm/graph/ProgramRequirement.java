package org.jmouse.vm.graph;

/**
 * An immutable, value-equal fact required by a compilation. Extensions may introduce facts for
 * runtime components, resources or language-specific dependencies without changing the solver.
 */
public interface ProgramRequirement {}
