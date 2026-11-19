package org.jmouse.vm.graph.dependency;

import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.vm.graph.DependencyProvider;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.MethodDependencyProvider;
import org.jmouse.vm.graph.ProgramDependencyException;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.ClassInitializationRequirement;
import org.jmouse.vm.graph.requirement.MethodImplementationRequirement;
import org.jmouse.vm.graph.requirement.TypeReachabilityRequirement;
import org.jmouse.vm.program.ProgramHierarchy;
import org.jmouse.vm.program.ProgramMethodDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class MethodImplementationDependencies implements DependencyProvider {
    private final ProgramHierarchy hierarchy;
    private final MethodDependencyProvider bodies;

    public MethodImplementationDependencies(
            ProgramHierarchy hierarchy, MethodDependencyProvider bodies) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
        this.bodies = Objects.requireNonNull(bodies, "bodies");
    }

    @Override
    public Iterable<DependencyRule> dependencies(ProgramRequirement requirement) {
        if (!(requirement instanceof MethodImplementationRequirement implementation)) {
            return List.of();
        }
        DependencyOrigin origin =
                DependencyOrigin.forType(hierarchy, implementation.method().owner());
        List<DependencyRule> rules = new ArrayList<>();
        ProgramMethodDeclaration method =
                hierarchy
                        .declarations()
                        .findMethod(implementation.method())
                        .orElseThrow(
                                () ->
                                        new ProgramDependencyException(
                                                requirement, "Missing method declaration"));
        origin.add(
                rules,
                requirement,
                new TypeReachabilityRequirement(method.key().owner()),
                StandardDependencyKind.DECLARATION);
        if (method.modifiers().contains(JavaStandardModifier.STATIC)
                && !method.key().name().equals(JavaJvmNames.CLASS_INITIALIZER)) {
            origin.add(
                    rules,
                    requirement,
                    new ClassInitializationRequirement(method.key().owner()),
                    StandardDependencyKind.CLASS_INITIALIZATION);
        }

        return () ->
                Stream.concat(
                                rules.stream(),
                                StreamSupport.stream(
                                        bodies.dependencies(method.key()).spliterator(), false))
                        .iterator();
    }
}
