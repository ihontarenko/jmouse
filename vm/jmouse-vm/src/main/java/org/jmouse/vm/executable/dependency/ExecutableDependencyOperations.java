package org.jmouse.vm.executable.dependency;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.vm.executable.node.ExecutableBasicBlockNode;
import org.jmouse.vm.executable.node.ExecutableBlockNode;
import org.jmouse.vm.executable.node.ExecutableBranchTerminatorNode;
import org.jmouse.vm.executable.node.ExecutableCallNode;
import org.jmouse.vm.executable.node.ExecutableCatchNode;
import org.jmouse.vm.executable.node.ExecutableControlFlowNode;
import org.jmouse.vm.executable.node.ExecutableEvaluationNode;
import org.jmouse.vm.executable.node.ExecutableIfNode;
import org.jmouse.vm.executable.node.ExecutableJumpTerminatorNode;
import org.jmouse.vm.executable.node.ExecutableLocalReferenceNode;
import org.jmouse.vm.executable.node.ExecutableNode;
import org.jmouse.vm.executable.node.ExecutableResourceNode;
import org.jmouse.vm.executable.node.ExecutableReturnTerminatorNode;
import org.jmouse.vm.executable.node.ExecutableTryNode;
import org.jmouse.vm.graph.DependencyRule;

import java.util.List;

/**
 * Exact dependency operations; an unregistered contributed node is refused by the shared registry.
 */
public final class ExecutableDependencyOperations {

    private ExecutableDependencyOperations() {}

    public static NodeOperationRegistry<ExecutableDependencyContext, Iterable<DependencyRule>>
            standard() {
        NodeOperationRegistry<ExecutableDependencyContext, Iterable<DependencyRule>> operations =
                new NodeOperationRegistry<>(ExecutableDependencyNames.OPERATION);
        structural(operations, ExecutableBlockNode.KIND);
        structural(operations, ExecutableIfNode.KIND);
        structural(operations, ExecutableTryNode.KIND);
        structural(operations, ExecutableLocalReferenceNode.KIND);
        structural(operations, ExecutableControlFlowNode.KIND);
        structural(operations, ExecutableJumpTerminatorNode.KIND);
        structural(operations, ExecutableBranchTerminatorNode.KIND);
        structural(operations, ExecutableReturnTerminatorNode.KIND);
        structural(operations, ExecutableEvaluationNode.KIND);
        operations.bind(
                ExecutableResourceNode.KIND,
                ExecutableResourceDependencies.class.getName(),
                new ExecutableResourceDependencies());
        operations.bind(
                ExecutableCatchNode.KIND,
                ExecutableCatchDependencies.class.getName(),
                new ExecutableCatchDependencies());
        operations.bind(
                ExecutableBasicBlockNode.KIND,
                ExecutableBasicBlockDependencies.class.getName(),
                new ExecutableBasicBlockDependencies());
        operations.bind(
                ExecutableCallNode.KIND,
                ExecutableCallDependencies.class.getName(),
                new ExecutableCallDependencies());
        return operations.snapshot();
    }

    private static <N extends ExecutableNode> void structural(
            NodeOperationRegistry<ExecutableDependencyContext, Iterable<DependencyRule>> operations,
            NodeKind<N> kind) {
        operations.bind(
                kind, ExecutableDependencyOperations.class.getName(), (node, context) -> List.of());
    }
}
