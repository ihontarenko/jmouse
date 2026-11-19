package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

public final class ExecutableEvaluationNode extends ExecutableOperationNode {

    public static final NodeKind<ExecutableEvaluationNode> KIND =
            new NodeKind<>(ExecutableNodeNames.EVALUATION, ExecutableEvaluationNode.class);
    public static final ChildRole<ExecutableExpressionNode> VALUE =
            ChildRole.required(ExecutableNodeNames.VALUE, ExecutableExpressionNode.class);
    public static final NodeSchema<ExecutableEvaluationNode> SCHEMA = NodeSchema.of(KIND, VALUE);

    public ExecutableEvaluationNode(
            ExecutableNodeMetadata metadata, ExecutableExpressionNode value) {
        super(SCHEMA, metadata);
        setChild(VALUE, value);
    }

    public ExecutableExpressionNode value() {
        return child(VALUE);
    }
}
