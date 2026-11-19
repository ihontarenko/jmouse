package org.jmouse.vm.executable.node;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.ExecutableNodeMetadata;

/** Ordered operation, usable as a structured statement or inside one basic block. */
public abstract class ExecutableOperationNode extends ExecutableStatementNode {

    protected ExecutableOperationNode(NodeSchema<?> schema, ExecutableNodeMetadata metadata) {
        super(schema, metadata);
    }
}
