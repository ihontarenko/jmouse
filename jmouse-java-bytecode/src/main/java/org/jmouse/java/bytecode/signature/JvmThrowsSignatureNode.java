package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

public final class JvmThrowsSignatureNode extends JvmTextNode {
    public static final NodeKind<JvmThrowsSignatureNode> KIND =
            new NodeKind<>(JvmTextNames.THROWS_SIGNATURE, JvmThrowsSignatureNode.class);
    public static final ChildRole<JvmReferenceTypeSignatureNode> TYPE =
            ChildRole.required(JvmTextNames.TYPE, JvmReferenceTypeSignatureNode.class);
    public static final NodeSchema<JvmThrowsSignatureNode> SCHEMA = NodeSchema.of(KIND, TYPE);

    public JvmThrowsSignatureNode(
            TokenizableSource source, int start, int end, JvmReferenceTypeSignatureNode type) {
        super(SCHEMA, source, start, end);

        setChild(TYPE, type);
    }

    public JvmReferenceTypeSignatureNode type() {
        return child(TYPE);
    }

    @Override
    protected void validateStructure() {
        if (!(type() instanceof JvmClassTypeSignatureNode)
                && !(type() instanceof JvmTypeVariableSignatureNode)) {
            throw new IllegalStateException("A throws signature requires a class or type variable");
        }
    }
}
