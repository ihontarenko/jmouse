package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.BasicBlockId;
import org.jmouse.vm.executable.ExceptionHandlerEdge;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ExecutableControlFlowNode extends ExecutableNode {

    public static final NodeKind<ExecutableControlFlowNode> KIND =
            new NodeKind<>(ExecutableNodeNames.CONTROL_FLOW, ExecutableControlFlowNode.class);
    public static final ChildRole<ExecutableBasicBlockNode> BLOCKS =
            ChildRole.list(ExecutableNodeNames.BLOCKS, ExecutableBasicBlockNode.class);
    public static final NodeSchema<ExecutableControlFlowNode> SCHEMA = NodeSchema.of(KIND, BLOCKS);

    private final BasicBlockId entry;

    public ExecutableControlFlowNode(ExecutableNodeMetadata metadata, BasicBlockId entry) {
        super(SCHEMA, metadata);
        this.entry = Objects.requireNonNull(entry, "entry");
    }

    public BasicBlockId entry() {
        return entry;
    }

    public List<ExecutableBasicBlockNode> blocks() {
        return children(BLOCKS);
    }

    public void addBlock(ExecutableBasicBlockNode block) {
        addChild(BLOCKS, block);
    }

    @Override
    protected void validateStructure() {
        Set<BasicBlockId> identities = new HashSet<>();
        for (ExecutableBasicBlockNode block : blocks()) {
            if (!block.identity().owner().equals(metadata().identity().owner())
                    || !identities.add(block.identity())) {
                throw new IllegalStateException("Foreign or repeated basic block identity");
            }
        }
        requireTarget(identities, entry);
        for (ExecutableBasicBlockNode block : blocks()) {
            for (BasicBlockId target : block.terminator().successors()) {
                requireTarget(identities, target);
            }
            for (ExceptionHandlerEdge handler : block.exceptionHandlers()) {
                requireTarget(identities, handler.target());
            }
        }
    }

    private static void requireTarget(Set<BasicBlockId> identities, BasicBlockId target) {
        if (!identities.contains(target)) {
            throw new IllegalStateException("Missing or foreign basic block target: " + target);
        }
    }
}
