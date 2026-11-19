package org.jmouse.vm.executable.node;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.BasicBlockId;
import org.jmouse.vm.executable.ExecutableNodeMetadata;

import java.util.List;

/** Ends one basic block; exceptional successors are separate ordered handler edges. */
public abstract class ExecutableTerminatorNode extends ExecutableNode {

    protected ExecutableTerminatorNode(NodeSchema<?> schema, ExecutableNodeMetadata metadata) {
        super(schema, metadata);
    }

    public abstract List<BasicBlockId> successors();
}
