package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.vm.executable.BasicBlockId;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

import java.util.List;
import java.util.Objects;

public final class ExecutableBranchTerminatorNode extends ExecutableTerminatorNode {

    public static final NodeKind<ExecutableBranchTerminatorNode> KIND =
            new NodeKind<>(
                    ExecutableNodeNames.BRANCH_TERMINATOR, ExecutableBranchTerminatorNode.class);
    public static final ChildRole<ExecutableExpressionNode> CONDITION =
            ChildRole.required(ExecutableNodeNames.CONDITION, ExecutableExpressionNode.class);
    public static final NodeSchema<ExecutableBranchTerminatorNode> SCHEMA =
            NodeSchema.of(KIND, CONDITION);

    private final BasicBlockId consequent;
    private final BasicBlockId alternative;

    public ExecutableBranchTerminatorNode(
            ExecutableNodeMetadata metadata,
            ExecutableExpressionNode condition,
            BasicBlockId consequent,
            BasicBlockId alternative) {
        super(SCHEMA, metadata);
        this.consequent = Objects.requireNonNull(consequent, "consequent");
        this.alternative = Objects.requireNonNull(alternative, "alternative");
        setChild(CONDITION, condition);
    }

    public ExecutableExpressionNode condition() {
        return child(CONDITION);
    }

    public BasicBlockId consequent() {
        return consequent;
    }

    public BasicBlockId alternative() {
        return alternative;
    }

    @Override
    public List<BasicBlockId> successors() {
        return List.of(consequent, alternative);
    }

    @Override
    protected void validateStructure() {
        if (condition().type() != JavaPrimitiveType.BOOLEAN) {
            throw new IllegalStateException(
                    "A branch requires an explicitly converted boolean condition");
        }
    }
}
