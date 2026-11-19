package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.BasicBlockId;
import org.jmouse.vm.executable.ExceptionHandlerEdge;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A block has one ordered handler list; frontends split blocks when handler coverage changes. */
public final class ExecutableBasicBlockNode extends ExecutableNode {

    public static final NodeKind<ExecutableBasicBlockNode> KIND =
            new NodeKind<>(ExecutableNodeNames.BASIC_BLOCK, ExecutableBasicBlockNode.class);
    public static final ChildRole<ExecutableOperationNode> OPERATIONS =
            ChildRole.list(ExecutableNodeNames.OPERATIONS, ExecutableOperationNode.class);
    public static final ChildRole<ExecutableTerminatorNode> TERMINATOR =
            ChildRole.required(ExecutableNodeNames.TERMINATOR, ExecutableTerminatorNode.class);
    public static final NodeSchema<ExecutableBasicBlockNode> SCHEMA =
            NodeSchema.of(KIND, OPERATIONS, TERMINATOR);

    private final BasicBlockId identity;
    private final List<ExceptionHandlerEdge> exceptionHandlers = new ArrayList<>();

    public ExecutableBasicBlockNode(
            ExecutableNodeMetadata metadata,
            BasicBlockId identity,
            ExecutableTerminatorNode terminator) {
        super(SCHEMA, metadata);
        this.identity = Objects.requireNonNull(identity, "identity");
        if (!identity.owner().equals(metadata.identity().owner())) {
            throw new IllegalArgumentException("Block and node owners differ");
        }
        setChild(TERMINATOR, terminator);
    }

    public BasicBlockId identity() {
        return identity;
    }

    public List<ExecutableOperationNode> operations() {
        return children(OPERATIONS);
    }

    public void addOperation(ExecutableOperationNode operation) {
        addChild(OPERATIONS, operation);
    }

    public ExecutableTerminatorNode terminator() {
        return child(TERMINATOR);
    }

    public List<ExceptionHandlerEdge> exceptionHandlers() {
        return List.copyOf(exceptionHandlers);
    }

    public void addExceptionHandler(ExceptionHandlerEdge handler) {
        requireBuilding();
        exceptionHandlers.add(Objects.requireNonNull(handler, "handler"));
    }
}
