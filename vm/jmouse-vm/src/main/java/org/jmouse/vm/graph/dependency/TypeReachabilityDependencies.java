package org.jmouse.vm.graph.dependency;

import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.graph.DependencyProvider;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.TypeReachabilityRequirement;
import org.jmouse.vm.program.ProgramClassDeclaration;
import org.jmouse.vm.program.ProgramHierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TypeReachabilityDependencies implements DependencyProvider {
    private final ProgramHierarchy hierarchy;

    public TypeReachabilityDependencies(ProgramHierarchy hierarchy) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
    }

    @Override
    public Iterable<DependencyRule> dependencies(ProgramRequirement requirement) {
        if (!(requirement instanceof TypeReachabilityRequirement reachable)) {
            return List.of();
        }
        DependencyOrigin origin = DependencyOrigin.forType(hierarchy, reachable.type());
        List<DependencyRule> rules = new ArrayList<>();
        ProgramClassDeclaration declaration =
                hierarchy.declarations().requireType(reachable.type());
        declaration
                .superclass()
                .ifPresent(
                        parent ->
                                origin.add(
                                        rules,
                                        requirement,
                                        new TypeReachabilityRequirement(parent),
                                        StandardDependencyKind.DECLARATION));
        for (JavaTypeKey parent : declaration.interfaces()) {
            origin.add(
                    rules,
                    requirement,
                    new TypeReachabilityRequirement(parent),
                    StandardDependencyKind.DECLARATION);
        }
        return List.copyOf(rules);
    }
}
