package org.jmouse.vm.graph.dependency;

import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.vm.graph.DependencyProvider;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.ProgramDependencyException;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.MethodImplementationRequirement;
import org.jmouse.vm.graph.requirement.VirtualDispatchRequirement;
import org.jmouse.vm.program.ExecutableMethodKey;
import org.jmouse.vm.program.ProgramHierarchy;
import org.jmouse.vm.program.ProgramMethodDeclaration;
import org.jmouse.vm.program.VirtualDispatchStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VirtualDispatchDependencies implements DependencyProvider {
    private final ProgramHierarchy hierarchy;
    private final VirtualDispatchStrategy dispatch;

    public VirtualDispatchDependencies(
            ProgramHierarchy hierarchy, VirtualDispatchStrategy dispatch) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
        this.dispatch = Objects.requireNonNull(dispatch, "dispatch");
    }

    @Override
    public Iterable<DependencyRule> dependencies(ProgramRequirement requirement) {
        if (!(requirement instanceof VirtualDispatchRequirement selected)) {
            return List.of();
        }
        DependencyOrigin origin = DependencyOrigin.forType(hierarchy, selected.receiver());
        List<DependencyRule> rules = new ArrayList<>();
        ExecutableMethodKey method =
                dispatch.select(selected.receiver(), selected.resolvedMethod())
                        .orElseThrow(
                                () ->
                                        new ProgramDependencyException(
                                                requirement,
                                                "No virtual implementation was selected"));
        ProgramMethodDeclaration declaration =
                hierarchy
                        .declarations()
                        .findMethod(method)
                        .orElseThrow(
                                () ->
                                        new ProgramDependencyException(
                                                requirement,
                                                "Dispatch policy selected a missing"
                                                        + " declaration"));
        if (declaration.modifiers().contains(JavaStandardModifier.ABSTRACT)
                || declaration.modifiers().contains(JavaStandardModifier.STATIC)) {
            throw new ProgramDependencyException(
                    requirement, "Dispatch policy selected an abstract or static method");
        }
        origin.add(
                rules,
                requirement,
                new MethodImplementationRequirement(method),
                StandardDependencyKind.VIRTUAL_DISPATCH);
        return List.copyOf(rules);
    }
}
