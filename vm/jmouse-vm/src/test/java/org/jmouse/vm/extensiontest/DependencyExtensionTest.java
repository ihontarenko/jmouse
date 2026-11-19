package org.jmouse.vm.extensiontest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.jmouse.vm.graph.DependencyProvider;
import org.jmouse.vm.graph.DependencyReason;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.EntryPoint;
import org.jmouse.vm.graph.ProgramDependencies;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.ReachabilityAnalyzer;
import org.jmouse.vm.graph.ReachabilityResult;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class DependencyExtensionTest {

    @Test
    void anExternalContributorAddsTransitiveRuntimeDependenciesWithProvenance() {
        ProgramRequirement entry = new ExtensionRequirement("entry");
        ProgramRequirement replacement = new ExtensionRequirement("replacement");
        ProgramRequirement runtime = new ExtensionRequirement("runtime");
        DependencyReason substitution =
                new DependencyReason(
                        StandardDependencyKind.SUBSTITUTION,
                        "External replacement binding",
                        List.of());
        DependencyReason runtimeReason =
                new DependencyReason(
                        StandardDependencyKind.RUNTIME,
                        "Replacement needs its runtime component",
                        List.of());
        DependencyRule replacementRule =
                new DependencyRule(Set.of(entry), replacement, substitution);
        DependencyRule runtimeRule =
                new DependencyRule(Set.of(replacement), runtime, runtimeReason);
        DependencyProvider extension =
                requirement -> {
                    if (requirement.equals(entry)) {
                        return List.of(replacementRule);
                    }
                    return requirement.equals(replacement) ? List.of(runtimeRule) : List.of();
                };
        List<DependencyProvider> registrations = new ArrayList<>(List.of(extension));
        ProgramDependencies configuration = new ProgramDependencies(registrations);
        registrations.clear();
        ReachabilityAnalyzer analyzer = new ReachabilityAnalyzer(List.of(configuration));

        ReachabilityResult result =
                analyzer.analyze(
                        List.of(
                                new EntryPoint(
                                        entry,
                                        new DependencyReason(
                                                StandardDependencyKind.ENTRY_POINT,
                                                "Explicit root",
                                                List.of()))));

        assertEquals(Set.of(entry, replacement, runtime), result.requirements());
        assertEquals(Set.of(runtimeRule), result.reasonsFor(runtime));
        assertFalse(analyzer.analyze(List.of()).contains(runtime));
    }

    private record ExtensionRequirement(String identity) implements ProgramRequirement {}
}
