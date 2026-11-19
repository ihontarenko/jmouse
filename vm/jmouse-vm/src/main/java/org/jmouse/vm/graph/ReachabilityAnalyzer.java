package org.jmouse.vm.graph;

import org.jmouse.core.graph.DirectedMapGraph;
import org.jmouse.core.graph.Graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;

/** Monotonic dependency closure with indexed conjunctive rules and explicit provenance. */
public final class ReachabilityAnalyzer {

    private final List<DependencyProvider> providers;
    private final ReachabilityLimits limits;

    public ReachabilityAnalyzer(List<DependencyProvider> providers) {
        this(providers, ReachabilityLimits.defaults());
    }

    public ReachabilityAnalyzer(List<DependencyProvider> providers, ReachabilityLimits limits) {
        this.providers = List.copyOf(providers);
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public ReachabilityResult analyze(Collection<EntryPoint> entryPoints) {
        return new Analysis().analyze(Objects.requireNonNull(entryPoints, "entryPoints"));
    }

    private final class Analysis {

        private final Set<ProgramRequirement> requirements = new HashSet<>();
        private final Set<DependencyRule> registered = new HashSet<>();
        private final Map<ProgramRequirement, Set<DependencyRule>> reasons = new HashMap<>();
        private final Map<ProgramRequirement, List<PendingRule>> waiting = new HashMap<>();
        private final Deque<ProgramRequirement> pending = new ArrayDeque<>();
        private final Graph<ProgramRequirement> graph = new DirectedMapGraph<>();
        private long work;

        private ReachabilityResult analyze(Collection<EntryPoint> entryPoints) {
            for (EntryPoint entryPoint : entryPoints) {
                register(
                        new DependencyRule(
                                Set.of(), entryPoint.requirement(), entryPoint.reason()));
            }
            while (!pending.isEmpty()) {
                step();
                ProgramRequirement current = pending.removeFirst();
                List<PendingRule> listeners = waiting.remove(current);
                if (listeners != null) {
                    for (PendingRule listener : listeners) {
                        step();
                        if (--listener.remaining == 0) {
                            activate(listener.rule);
                        }
                    }
                }
                for (DependencyProvider provider : providers) {
                    step();
                    for (DependencyRule rule : provider.dependencies(current)) {
                        if (!rule.prerequisites().contains(current)) {
                            throw new IllegalArgumentException(
                                    "Dependency rule must include its discovery requirement: "
                                            + current);
                        }
                        register(rule);
                    }
                }
            }
            return new ReachabilityResult(requirements, reasons, graph);
        }

        private void register(DependencyRule rule) {
            step();
            if (registered.contains(rule)) {
                return;
            }
            check(ReachabilityLimit.RULES, registered.size(), limits.maximumRules());
            registered.add(rule);
            PendingRule registration = new PendingRule(rule);
            for (ProgramRequirement prerequisite : rule.prerequisites()) {
                step();
                if (!requirements.contains(prerequisite)) {
                    registration.remaining++;
                    waiting.computeIfAbsent(prerequisite, ignored -> new ArrayList<>())
                            .add(registration);
                }
            }
            if (registration.remaining == 0) {
                activate(rule);
            }
        }

        private void activate(DependencyRule rule) {
            step();
            ProgramRequirement required = rule.required();
            if (!requirements.contains(required)) {
                check(
                        ReachabilityLimit.REQUIREMENTS,
                        requirements.size(),
                        limits.maximumRequirements());
                requirements.add(required);
                graph.addNode(required);
                pending.addLast(required);
            }
            reasons.computeIfAbsent(required, ignored -> new HashSet<>()).add(rule);
            for (ProgramRequirement prerequisite : rule.prerequisites()) {
                step();
                graph.addEdge(prerequisite, required);
            }
        }

        private void step() {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Reachability analysis interrupted");
            }
            check(ReachabilityLimit.WORK, work, limits.maximumWork());
            work++;
        }

        private void check(ReachabilityLimit limit, long current, long maximum) {
            if (current >= maximum) {
                throw new ReachabilityLimitException(limit, maximum);
            }
        }
    }

    private static final class PendingRule {

        private final DependencyRule rule;
        private int remaining;

        private PendingRule(DependencyRule rule) {
            this.rule = rule;
        }
    }
}
