package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

import java.util.Optional;

public final class JvmTypeArgumentNode extends JvmTextNode {
    public static final NodeKind<JvmTypeArgumentNode> KIND =
            new NodeKind<>(JvmTextNames.TYPE_ARGUMENT, JvmTypeArgumentNode.class);
    public static final ChildRole<JvmReferenceTypeSignatureNode> BOUND =
            ChildRole.optional(JvmTextNames.BOUND, JvmReferenceTypeSignatureNode.class);
    public static final NodeSchema<JvmTypeArgumentNode> SCHEMA = NodeSchema.of(KIND, BOUND);
    private final JvmTypeArgumentVariance variance;

    public JvmTypeArgumentNode(
            TokenizableSource source,
            int start,
            int end,
            JvmTypeArgumentVariance variance,
            JvmReferenceTypeSignatureNode bound) {
        super(SCHEMA, source, start, end);
        this.variance = variance;
        setChild(BOUND, bound);
    }

    public JvmTypeArgumentVariance variance() {
        return variance;
    }

    public Optional<JvmReferenceTypeSignatureNode> bound() {
        return optionalChild(BOUND);
    }

    @Override
    protected void validateStructure() {
        if ((variance == JvmTypeArgumentVariance.UNBOUNDED) == bound().isPresent()) {
            throw new IllegalStateException("Only an unbounded wildcard omits its bound");
        }
    }
}
