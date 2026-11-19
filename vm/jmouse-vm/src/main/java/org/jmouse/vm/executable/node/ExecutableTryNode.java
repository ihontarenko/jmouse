package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;

import java.util.List;
import java.util.Optional;

/** Resources and catches retain declaration order; resource closing reverses resource order. */
public final class ExecutableTryNode extends ExecutableStatementNode {

    public static final NodeKind<ExecutableTryNode> KIND =
            new NodeKind<>(ExecutableNodeNames.TRY, ExecutableTryNode.class);
    public static final ChildRole<ExecutableResourceNode> RESOURCES =
            ChildRole.list(ExecutableNodeNames.RESOURCES, ExecutableResourceNode.class);
    public static final ChildRole<ExecutableBlockNode> BODY =
            ChildRole.required(ExecutableNodeNames.BODY, ExecutableBlockNode.class);
    public static final ChildRole<ExecutableCatchNode> CATCHES =
            ChildRole.list(ExecutableNodeNames.CATCHES, ExecutableCatchNode.class);
    public static final ChildRole<ExecutableBlockNode> FINALLY_BODY =
            ChildRole.optional(ExecutableNodeNames.FINALLY_BODY, ExecutableBlockNode.class);
    public static final NodeSchema<ExecutableTryNode> SCHEMA =
            NodeSchema.of(KIND, RESOURCES, BODY, CATCHES, FINALLY_BODY);

    public ExecutableTryNode(ExecutableNodeMetadata metadata, ExecutableBlockNode body) {
        super(SCHEMA, metadata);
        setChild(BODY, body);
    }

    public List<ExecutableResourceNode> resources() {
        return children(RESOURCES);
    }

    public void addResource(ExecutableResourceNode resource) {
        addChild(RESOURCES, resource);
    }

    public ExecutableBlockNode body() {
        return child(BODY);
    }

    public List<ExecutableCatchNode> catches() {
        return children(CATCHES);
    }

    public void addCatch(ExecutableCatchNode caught) {
        addChild(CATCHES, caught);
    }

    public Optional<ExecutableBlockNode> finallyBody() {
        return optionalChild(FINALLY_BODY);
    }

    public void setFinallyBody(ExecutableBlockNode body) {
        setChild(FINALLY_BODY, body);
    }

    @Override
    protected void validateStructure() {
        if (resources().isEmpty() && catches().isEmpty() && finallyBody().isEmpty()) {
            throw new IllegalStateException("Executable try requires resources, catch or finally");
        }
    }
}
