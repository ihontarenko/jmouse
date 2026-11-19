package org.jmouse.vm.graph;

import java.util.Set;

/** A finite proof subgraph, including all contributing roots and cyclic dependencies. */
public record ReachabilityExplanation(
        Set<ProgramRequirement> requirements, Set<DependencyRule> rules) {

    public ReachabilityExplanation {
        requirements = Set.copyOf(requirements);
        rules = Set.copyOf(rules);
    }
}
