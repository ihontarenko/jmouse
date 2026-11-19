package org.jmouse.vm.executable.node;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.ExecutableLocal;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

public final class ExecutableLocalReferenceNode extends ExecutableExpressionNode {

    public static final NodeKind<ExecutableLocalReferenceNode> KIND =
            new NodeKind<>(ExecutableNodeNames.LOCAL_REFERENCE, ExecutableLocalReferenceNode.class);
    public static final NodeSchema<ExecutableLocalReferenceNode> SCHEMA = NodeSchema.of(KIND);

    private final ExecutableLocal local;

    public ExecutableLocalReferenceNode(ExecutableNodeMetadata metadata, ExecutableLocal local) {
        super(SCHEMA, metadata, local.type());
        this.local = local;
        if (!metadata.identity().owner().equals(local.identity().owner())) {
            throw new IllegalArgumentException("A local reference cannot cross physical methods");
        }
    }

    public ExecutableLocal local() {
        return local;
    }
}
