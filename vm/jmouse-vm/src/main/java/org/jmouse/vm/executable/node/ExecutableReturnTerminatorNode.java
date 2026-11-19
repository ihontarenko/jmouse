package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.BasicBlockId;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

import java.util.List;
import java.util.Optional;

public final class ExecutableReturnTerminatorNode extends ExecutableTerminatorNode {

    public static final NodeKind<ExecutableReturnTerminatorNode> KIND =
            new NodeKind<>(
                    ExecutableNodeNames.RETURN_TERMINATOR, ExecutableReturnTerminatorNode.class);
    public static final ChildRole<ExecutableExpressionNode> VALUE =
            ChildRole.optional(ExecutableNodeNames.VALUE, ExecutableExpressionNode.class);
    public static final NodeSchema<ExecutableReturnTerminatorNode> SCHEMA =
            NodeSchema.of(KIND, VALUE);

    public ExecutableReturnTerminatorNode(ExecutableNodeMetadata metadata) {
        super(SCHEMA, metadata);
    }

    public Optional<ExecutableExpressionNode> value() {
        return optionalChild(VALUE);
    }

    public void setValue(ExecutableExpressionNode value) {
        setChild(VALUE, value);
    }

    @Override
    public List<BasicBlockId> successors() {
        return List.of();
    }
}
