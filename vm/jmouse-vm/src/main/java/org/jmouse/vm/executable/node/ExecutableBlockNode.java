package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

import java.util.List;

public final class ExecutableBlockNode extends ExecutableStatementNode {

    public static final NodeKind<ExecutableBlockNode> KIND =
            new NodeKind<>(ExecutableNodeNames.BLOCK, ExecutableBlockNode.class);
    public static final ChildRole<ExecutableStatementNode> STATEMENTS =
            ChildRole.list(ExecutableNodeNames.STATEMENTS, ExecutableStatementNode.class);
    public static final NodeSchema<ExecutableBlockNode> SCHEMA = NodeSchema.of(KIND, STATEMENTS);

    public ExecutableBlockNode(ExecutableNodeMetadata metadata) {
        super(SCHEMA, metadata);
    }

    public List<ExecutableStatementNode> statements() {
        return children(STATEMENTS);
    }

    public void addStatement(ExecutableStatementNode statement) {
        addChild(STATEMENTS, statement);
    }
}
