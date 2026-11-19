package org.jmouse.vm.executable.dependency;

import org.jmouse.el.translate.NodeOperation;
import org.jmouse.vm.executable.node.ExecutableBasicBlockNode;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.TypeReachabilityRequirement;

public final class ExecutableBasicBlockDependencies
        implements NodeOperation<
                ExecutableBasicBlockNode, ExecutableDependencyContext, Iterable<DependencyRule>> {

    @Override
    public Iterable<DependencyRule> apply(
            ExecutableBasicBlockNode node, ExecutableDependencyContext context) {
        return () ->
                node.exceptionHandlers().stream()
                        .flatMap(handler -> handler.exceptionType().stream())
                        .map(
                                type ->
                                        context.rule(
                                                node,
                                                new TypeReachabilityRequirement(type),
                                                StandardDependencyKind.DECLARATION))
                        .iterator();
    }
}
