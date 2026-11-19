package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.ExecutableLocal;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;
import org.jmouse.vm.executable.ResourceCloseBinding;

import java.util.Objects;

/** One evaluated resource value, retained until its ordered close operation. */
public final class ExecutableResourceNode extends ExecutableNode {

    public static final NodeKind<ExecutableResourceNode> KIND =
            new NodeKind<>(ExecutableNodeNames.RESOURCE, ExecutableResourceNode.class);
    public static final ChildRole<ExecutableExpressionNode> INITIALIZER =
            ChildRole.required(ExecutableNodeNames.INITIALIZER, ExecutableExpressionNode.class);
    public static final NodeSchema<ExecutableResourceNode> SCHEMA =
            NodeSchema.of(KIND, INITIALIZER);

    private final ExecutableLocal local;
    private final ResourceCloseBinding close;

    public ExecutableResourceNode(
            ExecutableNodeMetadata metadata,
            ExecutableLocal local,
            ExecutableExpressionNode initializer,
            ResourceCloseBinding close) {
        super(SCHEMA, metadata);
        this.local = Objects.requireNonNull(local, "local");
        this.close = Objects.requireNonNull(close, "close");
        if (!metadata.identity().owner().equals(local.identity().owner())) {
            throw new IllegalArgumentException("A resource local cannot cross physical methods");
        }
        setChild(INITIALIZER, initializer);
    }

    public ExecutableLocal local() {
        return local;
    }

    public ExecutableExpressionNode initializer() {
        return child(INITIALIZER);
    }

    public ResourceCloseBinding close() {
        return close;
    }
}
