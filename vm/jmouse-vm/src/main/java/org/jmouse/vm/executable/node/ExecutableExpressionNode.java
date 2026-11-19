package org.jmouse.vm.executable.node;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.type.JavaType;
import org.jmouse.vm.executable.ExecutableNodeMetadata;

import java.util.Objects;

public abstract class ExecutableExpressionNode extends ExecutableNode {

    private final JavaType type;

    protected ExecutableExpressionNode(
            NodeSchema<?> schema, ExecutableNodeMetadata metadata, JavaType type) {
        super(schema, metadata);
        this.type = Objects.requireNonNull(type, "type");
    }

    public final JavaType type() {
        return type;
    }
}
