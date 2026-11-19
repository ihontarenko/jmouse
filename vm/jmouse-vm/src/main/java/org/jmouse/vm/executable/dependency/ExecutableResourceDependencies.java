package org.jmouse.vm.executable.dependency;

import org.jmouse.el.translate.NodeOperation;
import org.jmouse.vm.executable.node.ExecutableResourceNode;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.RuntimeComponentRequirement;
import org.jmouse.vm.graph.requirement.VirtualInvocationRequirement;
import org.jmouse.vm.runtime.JavaRuntimeComponents;

import java.util.List;

public final class ExecutableResourceDependencies
        implements NodeOperation<
                ExecutableResourceNode, ExecutableDependencyContext, Iterable<DependencyRule>> {

    @Override
    public Iterable<DependencyRule> apply(
            ExecutableResourceNode node, ExecutableDependencyContext context) {
        return List.of(
                context.rule(
                        node,
                        new VirtualInvocationRequirement(node.close().method()),
                        StandardDependencyKind.CALL),
                context.rule(
                        node,
                        new RuntimeComponentRequirement(JavaRuntimeComponents.RESOURCE_CLOSING),
                        StandardDependencyKind.RUNTIME));
    }
}
