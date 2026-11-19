package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

import java.util.Optional;

public final class ExecutableIfNode extends ExecutableStatementNode {

    public static final NodeKind<ExecutableIfNode> KIND =
            new NodeKind<>(ExecutableNodeNames.IF, ExecutableIfNode.class);
    public static final ChildRole<ExecutableExpressionNode> CONDITION =
            ChildRole.required(ExecutableNodeNames.CONDITION, ExecutableExpressionNode.class);
    public static final ChildRole<ExecutableStatementNode> CONSEQUENT =
            ChildRole.required(ExecutableNodeNames.CONSEQUENT, ExecutableStatementNode.class);
    public static final ChildRole<ExecutableStatementNode> ALTERNATIVE =
            ChildRole.optional(ExecutableNodeNames.ALTERNATIVE, ExecutableStatementNode.class);
    public static final NodeSchema<ExecutableIfNode> SCHEMA =
            NodeSchema.of(KIND, CONDITION, CONSEQUENT, ALTERNATIVE);

    public ExecutableIfNode(
            ExecutableNodeMetadata metadata,
            ExecutableExpressionNode condition,
            ExecutableStatementNode consequent) {
        super(SCHEMA, metadata);
        setChild(CONDITION, condition);
        setChild(CONSEQUENT, consequent);
    }

    public ExecutableExpressionNode condition() {
        return child(CONDITION);
    }

    public ExecutableStatementNode consequent() {
        return child(CONSEQUENT);
    }

    public Optional<ExecutableStatementNode> alternative() {
        return optionalChild(ALTERNATIVE);
    }

    public void setAlternative(ExecutableStatementNode alternative) {
        setChild(ALTERNATIVE, alternative);
    }

    @Override
    protected void validateStructure() {
        if (condition().type() != JavaPrimitiveType.BOOLEAN) {
            throw new IllegalStateException(
                    "Executable if requires an explicitly converted boolean condition");
        }
    }
}
