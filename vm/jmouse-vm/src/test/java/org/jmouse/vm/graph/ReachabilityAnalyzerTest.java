package org.jmouse.vm.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

class ReachabilityAnalyzerTest {

    @Test
    void boundsAnUnendingRuleStreamBeforeMaterializingTheContributions() {
        ProgramRequirement root = new Requirement("root");
        DependencyProvider unending =
                active ->
                        () ->
                                Stream.iterate(0, index -> index + 1)
                                        .map(
                                                index ->
                                                        rule(
                                                                Set.of(active),
                                                                new Requirement(
                                                                        "generated-" + index)))
                                        .iterator();
        ReachabilityAnalyzer analyzer =
                new ReachabilityAnalyzer(List.of(unending), new ReachabilityLimits(100, 3, 1000));

        ReachabilityLimitException failure =
                assertThrows(
                        ReachabilityLimitException.class,
                        () -> analyzer.analyze(List.of(new EntryPoint(root, reason()))));

        assertEquals(ReachabilityLimit.RULES, failure.limit());
    }

    @Test
    void keepsEveryRootAndLateProofWithoutDependingOnRootOrder() {
        ProgramRequirement first = new Requirement("first");
        ProgramRequirement second = new Requirement("second");
        ProgramRequirement target = new Requirement("target");
        DependencyRule joined = rule(Set.of(first, second), target);
        DependencyRule alternative = rule(Set.of(second), target);
        DependencyProvider provider =
                active ->
                        active.equals(first)
                                ? List.of(joined)
                                : active.equals(second) ? List.of(alternative) : List.of();
        ReachabilityAnalyzer analyzer = new ReachabilityAnalyzer(List.of(provider));
        EntryPoint firstRoot = new EntryPoint(first, reason());
        EntryPoint secondRoot = new EntryPoint(second, reason());

        ReachabilityResult forward = analyzer.analyze(List.of(firstRoot, secondRoot));
        ReachabilityResult backward = analyzer.analyze(List.of(secondRoot, firstRoot));

        assertEquals(Set.of(joined, alternative), forward.reasonsFor(target));
        assertEquals(forward.explain(target), backward.explain(target));
        assertEquals(Set.of(target), forward.dependenciesFrom(first));
        assertThrows(UnsupportedOperationException.class, () -> forward.requirements().clear());
        assertThrows(UnsupportedOperationException.class, () -> forward.reasonsFor(target).clear());
    }

    @Test
    void boundsUnendingExtensionsAndDoesNotKeepFailedSessionState() {
        ProgramRequirement root = new Requirement("root");
        DependencyProvider unending =
                active ->
                        List.of(
                                rule(
                                        Set.of(active),
                                        new Requirement(((Requirement) active).name() + "+")));
        ReachabilityAnalyzer analyzer =
                new ReachabilityAnalyzer(List.of(unending), new ReachabilityLimits(3, 100, 1000));

        ReachabilityLimitException failure =
                assertThrows(
                        ReachabilityLimitException.class,
                        () -> analyzer.analyze(List.of(new EntryPoint(root, reason()))));

        assertEquals(ReachabilityLimit.REQUIREMENTS, failure.limit());
        assertEquals(3, failure.maximum());
        assertEquals(Set.of(), analyzer.analyze(List.of()).requirements());
    }

    @Test
    void refusesRulesThatCanIntroduceUnrelatedRoots() {
        ProgramRequirement root = new Requirement("root");
        DependencyProvider invalid =
                active -> List.of(rule(Set.of(), new Requirement("unrelated")));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ReachabilityAnalyzer(List.of(invalid))
                                .analyze(List.of(new EntryPoint(root, reason()))));
    }

    @Test
    void closesRecursiveDependenciesAndRequiresEveryVirtualDispatchPremise() {
        ProgramRequirement entry = new Requirement("entry");
        ProgramRequirement call = new Requirement("virtual call");
        ProgramRequirement receiver = new Requirement("created receiver");
        ProgramRequirement factory = new Requirement("receiver factory");
        ProgramRequirement implementation = new Requirement("implementation");
        ProgramRequirement initializer = new Requirement("initializer");
        ProgramRequirement unused = new Requirement("unused receiver");
        ProgramRequirement unusedImplementation = new Requirement("unused implementation");
        DependencyRule dispatch = rule(Set.of(call, receiver), implementation);
        DependencyRule initialize = rule(Set.of(implementation), initializer);
        DependencyProvider dependencies =
                active -> {
                    if (active.equals(entry)) {
                        return List.of(rule(Set.of(entry), call), rule(Set.of(entry), factory));
                    }
                    if (active.equals(factory)) {
                        return List.of(rule(Set.of(factory), receiver));
                    }
                    if (active.equals(call)) {
                        return List.of(dispatch, rule(Set.of(call, unused), unusedImplementation));
                    }
                    if (active.equals(implementation)) {
                        return List.of(initialize);
                    }
                    if (active.equals(initializer)) {
                        return List.of(rule(Set.of(initializer), implementation));
                    }
                    return List.of();
                };

        ReachabilityResult result =
                new ReachabilityAnalyzer(List.of(dependencies))
                        .analyze(List.of(new EntryPoint(entry, reason())));

        assertEquals(
                Set.of(entry, call, factory, receiver, implementation, initializer),
                result.requirements());
        assertFalse(result.contains(unusedImplementation));
        assertEquals(
                Set.of(dispatch, rule(Set.of(initializer), implementation)),
                result.reasonsFor(implementation));
        assertTrue(result.explain(initializer).rules().contains(initialize));
        assertEquals(result.requirements(), result.explain(initializer).requirements());
    }

    private static DependencyRule rule(
            Set<ProgramRequirement> prerequisites, ProgramRequirement required) {
        return new DependencyRule(prerequisites, required, reason());
    }

    private static DependencyReason reason() {
        return new DependencyReason(StandardDependencyKind.CALL, "Fixture dependency", List.of());
    }

    private record Requirement(String name) implements ProgramRequirement {}
}
