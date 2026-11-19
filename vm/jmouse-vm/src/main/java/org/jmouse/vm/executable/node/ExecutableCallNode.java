package org.jmouse.vm.executable.node;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.type.JavaType;
import org.jmouse.vm.executable.ExecutableInvocationKind;
import org.jmouse.vm.executable.ExecutableNodeMetadata;
import org.jmouse.vm.executable.ExecutableNodeNames;
import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolved call. Receiver evaluation precedes arguments; varargs/implicit arguments are explicit.
 */
public final class ExecutableCallNode extends ExecutableExpressionNode {

    public static final NodeKind<ExecutableCallNode> KIND =
            new NodeKind<>(ExecutableNodeNames.CALL, ExecutableCallNode.class);
    public static final ChildRole<ExecutableExpressionNode> RECEIVER =
            ChildRole.optional(ExecutableNodeNames.RECEIVER, ExecutableExpressionNode.class);
    public static final ChildRole<ExecutableExpressionNode> ARGUMENTS =
            ChildRole.list(ExecutableNodeNames.ARGUMENTS, ExecutableExpressionNode.class);
    public static final NodeSchema<ExecutableCallNode> SCHEMA =
            NodeSchema.of(KIND, RECEIVER, ARGUMENTS);

    private final ExecutableMethodKey target;
    private final ExecutableInvocationKind invocation;

    public ExecutableCallNode(
            ExecutableNodeMetadata metadata,
            JavaType type,
            ExecutableMethodKey target,
            ExecutableInvocationKind invocation) {
        super(SCHEMA, metadata, type);
        this.target = Objects.requireNonNull(target, "target");
        this.invocation = Objects.requireNonNull(invocation, "invocation");
    }

    public ExecutableMethodKey target() {
        return target;
    }

    public ExecutableInvocationKind invocation() {
        return invocation;
    }

    public Optional<ExecutableExpressionNode> receiver() {
        return optionalChild(RECEIVER);
    }

    public void setReceiver(ExecutableExpressionNode receiver) {
        setChild(RECEIVER, receiver);
    }

    public List<ExecutableExpressionNode> arguments() {
        return children(ARGUMENTS);
    }

    public void addArgument(ExecutableExpressionNode argument) {
        addChild(ARGUMENTS, argument);
    }

    @Override
    protected void validateStructure() {
        if ((invocation == ExecutableInvocationKind.STATIC) != receiver().isEmpty()
                || arguments().size() != target.descriptor().parameters().size()
                || target.name().equals(JavaJvmNames.CLASS_INITIALIZER)
                || (target.name().equals(JavaJvmNames.CONSTRUCTOR)
                        && invocation != ExecutableInvocationKind.SPECIAL)) {
            throw new IllegalStateException(
                    "Call receiver, arguments or invocation kind do not match its physical target");
        }
    }
}
