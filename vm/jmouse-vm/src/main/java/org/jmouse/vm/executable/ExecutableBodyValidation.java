package org.jmouse.vm.executable;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.NodeLifecycle;
import org.jmouse.el.node.Visitor;
import org.jmouse.vm.executable.node.ExecutableNode;
import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class ExecutableBodyValidation {

    private ExecutableBodyValidation() {}

    static void validate(ExecutableMethodKey owner, ExecutableNode root) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(root, "root");
        if (root.getParent() != null || root.lifecycle() != NodeLifecycle.FROZEN) {
            throw new IllegalArgumentException("A method body requires a frozen ownership root");
        }
        Set<ExecutableNodeId> identities = new HashSet<>();
        root.accept(
                new Visitor() {
                    @Override
                    public void visit(Node node) {
                        if (!(node instanceof ExecutableNode executable)
                                || !executable.metadata().identity().owner().equals(owner)
                                || !identities.add(executable.metadata().identity())) {
                            throw new IllegalArgumentException(
                                    "Foreign or duplicate executable node identity");
                        }
                    }
                });
    }
}
