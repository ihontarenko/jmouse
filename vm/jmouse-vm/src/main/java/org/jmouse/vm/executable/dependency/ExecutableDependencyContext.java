package org.jmouse.vm.executable.dependency;

import org.jmouse.vm.executable.ExecutableMethodBinding;
import org.jmouse.vm.executable.node.ExecutableNode;
import org.jmouse.vm.graph.DependencyKind;
import org.jmouse.vm.graph.DependencyReason;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.requirement.MethodImplementationRequirement;

import java.util.Objects;
import java.util.Set;

public record ExecutableDependencyContext(ExecutableMethodBinding implementation) {

    public ExecutableDependencyContext {
        Objects.requireNonNull(implementation, "implementation");
    }

    public DependencyRule rule(
            ExecutableNode node, ProgramRequirement required, DependencyKind kind) {
        String description =
                implementation.identity()
                        + ": "
                        + node.schema().kind().name()
                        + " requires "
                        + required;
        return new DependencyRule(
                Set.of(new MethodImplementationRequirement(implementation.method())),
                required,
                new DependencyReason(kind, description, node.metadata().origins()));
    }
}
