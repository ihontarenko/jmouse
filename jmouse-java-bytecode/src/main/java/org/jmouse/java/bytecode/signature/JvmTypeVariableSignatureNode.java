package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

public final class JvmTypeVariableSignatureNode extends JvmReferenceTypeSignatureNode {
    public static final NodeKind<JvmTypeVariableSignatureNode> KIND =
            new NodeKind<>(
                    JvmTextNames.TYPE_VARIABLE_SIGNATURE, JvmTypeVariableSignatureNode.class);

    public static final NodeSchema<JvmTypeVariableSignatureNode> SCHEMA = NodeSchema.of(KIND);
    private final String name;

    public JvmTypeVariableSignatureNode(TokenizableSource source, int start, int end, String name) {
        super(SCHEMA, source, start, end);
        this.name = name;
    }

    public String name() {
        return name;
    }
}
