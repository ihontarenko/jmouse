package org.jmouse.vm.executable.node;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.BasicBlockId;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

import java.util.List;
import java.util.Objects;

public final class ExecutableJumpTerminatorNode extends ExecutableTerminatorNode {

    public static final NodeKind<ExecutableJumpTerminatorNode> KIND =
            new NodeKind<>(ExecutableNodeNames.JUMP_TERMINATOR, ExecutableJumpTerminatorNode.class);
    public static final NodeSchema<ExecutableJumpTerminatorNode> SCHEMA = NodeSchema.of(KIND);

    private final BasicBlockId target;

    public ExecutableJumpTerminatorNode(ExecutableNodeMetadata metadata, BasicBlockId target) {
        super(SCHEMA, metadata);
        this.target = Objects.requireNonNull(target, "target");
    }

    public BasicBlockId target() {
        return target;
    }

    @Override
    public List<BasicBlockId> successors() {
        return List.of(target);
    }
}
