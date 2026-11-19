package org.jmouse.vm.executable.node;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.ExecutableNodeMetadata;

public abstract class ExecutableStatementNode extends ExecutableNode {

    protected ExecutableStatementNode(NodeSchema<?> schema, ExecutableNodeMetadata metadata) {
        super(schema, metadata);
    }
}
