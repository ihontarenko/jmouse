package org.jmouse.vm.graph;

import org.jmouse.core.graph.Graph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable closure of the contributed dependency rules. Executable-body validation and linking
 * remain separate obligations. No partially explored graph is published after a failure.
 */
public final class ReachabilityResult {

    private final Set<ProgramRequirement> requirements;
    private final Map<ProgramRequirement, Set<DependencyRule>> reasons;
    private final Map<ProgramRequirement, Set<ProgramRequirement>> dependencies;

    ReachabilityResult(
            Set<ProgramRequirement> requirements,
            Map<ProgramRequirement, Set<DependencyRule>> reasons,
            Graph<ProgramRequirement> graph) {
        this.requirements = Set.copyOf(requirements);
        Map<ProgramRequirement, Set<DependencyRule>> copiedReasons = new HashMap<>();
        Map<ProgramRequirement, Set<ProgramRequirement>> copiedDependencies = new HashMap<>();
        for (ProgramRequirement requirement : requirements) {
            copiedReasons.put(requirement, Set.copyOf(reasons.get(requirement)));
            copiedDependencies.put(requirement, graph.getNeighbors(requirement));
        }
        this.reasons = Map.copyOf(copiedReasons);
        this.dependencies = Map.copyOf(copiedDependencies);
    }

    public Set<ProgramRequirement> requirements() {
        return requirements;
    }

    public boolean contains(ProgramRequirement requirement) {
        return requirements.contains(Objects.requireNonNull(requirement, "requirement"));
    }

    public Set<DependencyRule> reasonsFor(ProgramRequirement requirement) {
        return reasons.getOrDefault(Objects.requireNonNull(requirement, "requirement"), Set.of());
    }

    public Set<ProgramRequirement> dependenciesFrom(ProgramRequirement requirement) {
        return dependencies.getOrDefault(
                Objects.requireNonNull(requirement, "requirement"), Set.of());
    }

    public ReachabilityExplanation explain(ProgramRequirement requirement) {
        if (!contains(requirement)) {
            throw new IllegalArgumentException("Requirement is not reachable: " + requirement);
        }
        Set<ProgramRequirement> explained = new HashSet<>();
        Set<DependencyRule> rules = new HashSet<>();
        Deque<ProgramRequirement> pending = new ArrayDeque<>();
        pending.addLast(requirement);
        while (!pending.isEmpty()) {
            ProgramRequirement current = pending.removeFirst();
            if (explained.add(current)) {
                for (DependencyRule rule : reasonsFor(current)) {
                    rules.add(rule);
                    pending.addAll(rule.prerequisites());
                }
            }
        }
        return new ReachabilityExplanation(explained, rules);
    }
}
