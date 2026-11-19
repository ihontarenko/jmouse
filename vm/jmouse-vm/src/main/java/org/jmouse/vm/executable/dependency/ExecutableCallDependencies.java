package org.jmouse.vm.executable.dependency;

import org.jmouse.el.translate.NodeOperation;
import org.jmouse.vm.executable.node.ExecutableCallNode;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.MethodImplementationRequirement;
import org.jmouse.vm.graph.requirement.VirtualInvocationRequirement;

import java.util.List;

public final class ExecutableCallDependencies
        implements NodeOperation<
                ExecutableCallNode, ExecutableDependencyContext, Iterable<DependencyRule>> {

    @Override
    public Iterable<DependencyRule> apply(
            ExecutableCallNode node, ExecutableDependencyContext context) {
        ProgramRequirement target =
                switch (node.invocation()) {
                    case STATIC, SPECIAL -> new MethodImplementationRequirement(node.target());
                    case VIRTUAL, INTERFACE -> new VirtualInvocationRequirement(node.target());
                };
        return List.of(context.rule(node, target, StandardDependencyKind.CALL));
    }
}
