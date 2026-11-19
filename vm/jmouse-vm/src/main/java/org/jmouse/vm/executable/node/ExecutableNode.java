package org.jmouse.vm.executable.node;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.el.node.StructuredNode;
import org.jmouse.vm.executable.ExecutableNodeMetadata;

import java.util.Objects;

/** Target-independent operation/control structure, using the shared jME ownership and visitor. */
public abstract class ExecutableNode extends StructuredNode {

    private final ExecutableNodeMetadata metadata;

    protected ExecutableNode(NodeSchema<?> schema, ExecutableNodeMetadata metadata) {
        super(schema);
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    public final ExecutableNodeMetadata metadata() {
        return metadata;
    }
}
