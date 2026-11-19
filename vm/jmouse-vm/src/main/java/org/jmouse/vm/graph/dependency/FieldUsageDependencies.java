package org.jmouse.vm.graph.dependency;

import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.vm.graph.DependencyProvider;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.ProgramDependencyException;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.ClassInitializationRequirement;
import org.jmouse.vm.graph.requirement.FieldUsageRequirement;
import org.jmouse.vm.graph.requirement.TypeReachabilityRequirement;
import org.jmouse.vm.program.ProgramFieldDeclaration;
import org.jmouse.vm.program.ProgramHierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FieldUsageDependencies implements DependencyProvider {
    private final ProgramHierarchy hierarchy;

    public FieldUsageDependencies(ProgramHierarchy hierarchy) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
    }

    @Override
    public Iterable<DependencyRule> dependencies(ProgramRequirement requirement) {
        if (!(requirement instanceof FieldUsageRequirement usage)) {
            return List.of();
        }
        DependencyOrigin origin = DependencyOrigin.forType(hierarchy, usage.field().owner());
        List<DependencyRule> rules = new ArrayList<>();
        ProgramFieldDeclaration field =
                hierarchy
                        .declarations()
                        .findField(usage.field())
                        .orElseThrow(
                                () ->
                                        new ProgramDependencyException(
                                                requirement, "Missing field declaration"));
        origin.add(
                rules,
                requirement,
                new TypeReachabilityRequirement(field.key().owner()),
                StandardDependencyKind.DECLARATION);
        if (field.modifiers().contains(JavaStandardModifier.STATIC)) {
            origin.add(
                    rules,
                    requirement,
                    new ClassInitializationRequirement(field.key().owner()),
                    StandardDependencyKind.CLASS_INITIALIZATION);
        }
        return List.copyOf(rules);
    }
}
