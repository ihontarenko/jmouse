package org.jmouse.vm.graph.dependency;

import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.graph.DependencyProvider;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.ProgramDependencyException;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.ClassInitializationRequirement;
import org.jmouse.vm.graph.requirement.MethodImplementationRequirement;
import org.jmouse.vm.graph.requirement.TypeReachabilityRequirement;
import org.jmouse.vm.program.ClassInitializationPlan;
import org.jmouse.vm.program.ClassInitializationStrategy;
import org.jmouse.vm.program.ProgramHierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ClassInitializationDependencies implements DependencyProvider {
    private final ProgramHierarchy hierarchy;
    private final ClassInitializationStrategy initialization;

    public ClassInitializationDependencies(
            ProgramHierarchy hierarchy, ClassInitializationStrategy initialization) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
        this.initialization = Objects.requireNonNull(initialization, "initialization");
    }

    @Override
    public Iterable<DependencyRule> dependencies(ProgramRequirement requirement) {
        if (!(requirement instanceof ClassInitializationRequirement initialized)) {
            return List.of();
        }
        DependencyOrigin origin = DependencyOrigin.forType(hierarchy, initialized.type());
        List<DependencyRule> rules = new ArrayList<>();
        ClassInitializationPlan plan = initialization.plan(initialized.type());
        if (!plan.type().equals(initialized.type())) {
            throw new ProgramDependencyException(
                    requirement, "Initialization policy returned a different type");
        }
        origin.add(
                rules,
                requirement,
                new TypeReachabilityRequirement(initialized.type()),
                StandardDependencyKind.DECLARATION);
        for (JavaTypeKey prerequisite : plan.prerequisites()) {
            origin.add(
                    rules,
                    requirement,
                    new ClassInitializationRequirement(prerequisite),
                    StandardDependencyKind.CLASS_INITIALIZATION);
        }
        plan.initializer()
                .ifPresent(
                        method ->
                                origin.add(
                                        rules,
                                        requirement,
                                        new MethodImplementationRequirement(method),
                                        StandardDependencyKind.CLASS_INITIALIZATION));
        return List.copyOf(rules);
    }
}
