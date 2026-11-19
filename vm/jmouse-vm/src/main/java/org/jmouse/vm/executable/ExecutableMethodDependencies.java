package org.jmouse.vm.executable;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.TraversalAction;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.vm.executable.dependency.ExecutableDependencyContext;
import org.jmouse.vm.executable.dependency.ExecutableDependencyLimits;
import org.jmouse.vm.executable.dependency.ExecutableDependencyOperations;
import org.jmouse.vm.executable.node.ExecutableBasicBlockNode;
import org.jmouse.vm.executable.node.ExecutableNode;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.MethodDependencyProvider;
import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;

/** Dependencies of the selected body, using common traversal and exact per-node operations. */
public final class ExecutableMethodDependencies implements MethodDependencyProvider {

    private final ExecutableProgram program;
    private final NodeOperationRegistry<ExecutableDependencyContext, Iterable<DependencyRule>>
            operations;
    private final ExecutableDependencyLimits limits;

    public ExecutableMethodDependencies(ExecutableProgram program) {
        this(
                program,
                ExecutableDependencyOperations.standard(),
                ExecutableDependencyLimits.defaults());
    }

    public ExecutableMethodDependencies(
            ExecutableProgram program,
            NodeOperationRegistry<ExecutableDependencyContext, Iterable<DependencyRule>> operations,
            ExecutableDependencyLimits limits) {
        this.program = Objects.requireNonNull(program, "program");
        this.operations = Objects.requireNonNull(operations, "operations").snapshot();
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    @Override
    public Iterable<DependencyRule> dependencies(ExecutableMethodKey method) {
        return new Analysis(program.requireMethod(method)).collect();
    }

    private final class Analysis {

        private final ExecutableMethodBinding implementation;
        private final ExecutableDependencyContext context;
        private final List<DependencyRule> contributions = new ArrayList<>();
        private final Set<BasicBlockId> reachableBlocks = new HashSet<>();
        private long work;

        private Analysis(ExecutableMethodBinding implementation) {
            this.implementation = implementation;
            context = new ExecutableDependencyContext(implementation);
        }

        private List<DependencyRule> collect() {
            if (implementation.body() instanceof ControlFlowMethodBody body) {
                discoverBlocks(body);
            }
            implementation
                    .body()
                    .root()
                    .accept(
                            new Visitor() {
                                @Override
                                public TraversalAction enter(Node node) {
                                    step();
                                    return excluded(node)
                                            ? TraversalAction.SKIP_CHILDREN
                                            : TraversalAction.CONTINUE;
                                }

                                @Override
                                public void visit(Node node) {
                                    if (excluded(node)) {
                                        return;
                                    }
                                    for (DependencyRule rule :
                                            operations.apply((ExecutableNode) node, context)) {
                                        step();
                                        if (contributions.size() >= limits.maximumContributions()) {
                                            throw new IllegalStateException(
                                                    "Executable dependency contribution budget"
                                                            + " exceeded");
                                        }
                                        contributions.add(rule);
                                    }
                                }
                            });
            return List.copyOf(contributions);
        }

        private boolean excluded(Node node) {
            return node instanceof ExecutableBasicBlockNode block
                    && !reachableBlocks.contains(block.identity());
        }

        private void discoverBlocks(ControlFlowMethodBody body) {
            Deque<BasicBlockId> pending = new ArrayDeque<>();
            pending.addLast(body.root().entry());
            while (!pending.isEmpty()) {
                step();
                BasicBlockId block = pending.removeFirst();
                if (reachableBlocks.add(block)) {
                    for (BasicBlockId successor : body.normalSuccessors(block)) {
                        step();
                        pending.addLast(successor);
                    }
                    for (ExceptionHandlerEdge handler : body.exceptionHandlers(block)) {
                        step();
                        pending.addLast(handler.target());
                    }
                }
            }
        }

        private void step() {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Executable dependency analysis interrupted");
            }
            if (work >= limits.maximumWork()) {
                throw new IllegalStateException("Executable dependency work budget exceeded");
            }
            work++;
        }
    }
}
