package org.jmouse.vm.graph.dependency;

import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.graph.DependencyProvider;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.ProgramDependencyException;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.ClassInitializationRequirement;
import org.jmouse.vm.graph.requirement.TypeInstantiationRequirement;
import org.jmouse.vm.graph.requirement.TypeReachabilityRequirement;
import org.jmouse.vm.program.ProgramClassDeclaration;
import org.jmouse.vm.program.ProgramClassKind;
import org.jmouse.vm.program.ProgramHierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TypeInstantiationDependencies implements DependencyProvider {
    private final ProgramHierarchy hierarchy;

    public TypeInstantiationDependencies(ProgramHierarchy hierarchy) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
    }

    @Override
    public Iterable<DependencyRule> dependencies(ProgramRequirement requirement) {
        if (!(requirement instanceof TypeInstantiationRequirement(JavaTypeKey instantiated))) {
            return List.of();
        }
        DependencyOrigin origin =
                DependencyOrigin.forType(hierarchy, instantiated);
        List<DependencyRule> rules = new ArrayList<>();
        ProgramClassDeclaration declaration =
                hierarchy.declarations().requireType(instantiated);
        if (declaration.kind() != ProgramClassKind.CLASS
                || declaration.modifiers().contains(JavaStandardModifier.ABSTRACT)) {
            throw new ProgramDependencyException(
                    requirement, "Cannot instantiate an interface or abstract class");
        }
        origin.add(
                rules,
                requirement,
                new TypeReachabilityRequirement(instantiated),
                StandardDependencyKind.ALLOCATION);
        origin.add(
                rules,
                requirement,
                new ClassInitializationRequirement(instantiated),
                StandardDependencyKind.CLASS_INITIALIZATION);
        return List.copyOf(rules);
    }
}
