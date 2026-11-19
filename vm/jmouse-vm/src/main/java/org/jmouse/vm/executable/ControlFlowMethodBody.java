package org.jmouse.vm.executable;

import org.jmouse.core.graph.DirectedMapGraph;
import org.jmouse.core.graph.Graph;
import org.jmouse.vm.executable.node.ExecutableBasicBlockNode;
import org.jmouse.vm.executable.node.ExecutableControlFlowNode;
import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Frozen typed control-flow structure; stack/locals verification belongs to the frontend. */
public final class ControlFlowMethodBody implements ExecutableMethodBody {

    private final ExecutableMethodKey owner;
    private final ExecutableControlFlowNode root;
    private final Map<BasicBlockId, Set<BasicBlockId>> normalSuccessors;
    private final Map<BasicBlockId, List<ExceptionHandlerEdge>> exceptionHandlers;

    public ControlFlowMethodBody(ExecutableMethodKey owner, ExecutableControlFlowNode root) {
        ExecutableBodyValidation.validate(owner, root);
        this.owner = owner;
        this.root = root;
        Graph<BasicBlockId> graph = new DirectedMapGraph<>();
        Map<BasicBlockId, List<ExceptionHandlerEdge>> handlers = new HashMap<>();
        for (ExecutableBasicBlockNode block : root.blocks()) {
            graph.addNode(block.identity());
            block.terminator()
                    .successors()
                    .forEach(target -> graph.addEdge(block.identity(), target));
            handlers.put(block.identity(), block.exceptionHandlers());
        }
        Map<BasicBlockId, Set<BasicBlockId>> edges = new HashMap<>();
        for (ExecutableBasicBlockNode block : root.blocks()) {
            edges.put(block.identity(), graph.getNeighbors(block.identity()));
        }
        this.normalSuccessors = Map.copyOf(edges);
        this.exceptionHandlers = Map.copyOf(handlers);
    }

    @Override
    public ExecutableMethodKey owner() {
        return owner;
    }

    @Override
    public ExecutableControlFlowNode root() {
        return root;
    }

    public Set<BasicBlockId> normalSuccessors(BasicBlockId block) {
        requireBlock(block);
        return normalSuccessors.get(block);
    }

    public List<ExceptionHandlerEdge> exceptionHandlers(BasicBlockId block) {
        requireBlock(block);
        return exceptionHandlers.get(block);
    }

    private void requireBlock(BasicBlockId block) {
        if (!normalSuccessors.containsKey(Objects.requireNonNull(block, "block"))) {
            throw new IllegalArgumentException("Block does not belong to this body: " + block);
        }
    }
}
