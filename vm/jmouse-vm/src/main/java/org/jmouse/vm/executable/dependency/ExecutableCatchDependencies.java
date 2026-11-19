package org.jmouse.vm.executable.dependency;

import org.jmouse.el.translate.NodeOperation;
import org.jmouse.vm.executable.node.ExecutableCatchNode;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.TypeReachabilityRequirement;

public final class ExecutableCatchDependencies
        implements NodeOperation<
                ExecutableCatchNode, ExecutableDependencyContext, Iterable<DependencyRule>> {

    @Override
    public Iterable<DependencyRule> apply(
            ExecutableCatchNode node, ExecutableDependencyContext context) {
        return () ->
                node.alternatives().stream()
                        .map(
                                type ->
                                        context.rule(
                                                node,
                                                new TypeReachabilityRequirement(type),
                                                StandardDependencyKind.DECLARATION))
                        .iterator();
    }
}
