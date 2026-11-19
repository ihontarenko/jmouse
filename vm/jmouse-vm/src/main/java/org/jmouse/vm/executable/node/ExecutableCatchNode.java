package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.executable.ExecutableLocal;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class ExecutableCatchNode extends ExecutableNode {

    public static final NodeKind<ExecutableCatchNode> KIND =
            new NodeKind<>(ExecutableNodeNames.CATCH, ExecutableCatchNode.class);
    public static final ChildRole<ExecutableBlockNode> BODY =
            ChildRole.required(ExecutableNodeNames.BODY, ExecutableBlockNode.class);
    public static final NodeSchema<ExecutableCatchNode> SCHEMA = NodeSchema.of(KIND, BODY);

    private final List<JavaTypeKey> alternatives;
    private final ExecutableLocal exception;

    public ExecutableCatchNode(
            ExecutableNodeMetadata metadata,
            List<JavaTypeKey> alternatives,
            ExecutableLocal exception,
            ExecutableBlockNode body) {
        super(SCHEMA, metadata);
        this.alternatives = List.copyOf(alternatives);
        this.exception = Objects.requireNonNull(exception, "exception");
        if (alternatives.isEmpty()
                || new HashSet<>(alternatives).size() != alternatives.size()
                || !metadata.identity().owner().equals(exception.identity().owner())) {
            throw new IllegalArgumentException(
                    "A catch requires distinct types and a local in its physical method");
        }
        setChild(BODY, body);
    }

    public List<JavaTypeKey> alternatives() {
        return alternatives;
    }

    public ExecutableLocal exception() {
        return exception;
    }

    public ExecutableBlockNode body() {
        return child(BODY);
    }
}
