package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.NodeSchema;

public abstract class JvmReferenceTypeSignatureNode extends JvmTypeSyntaxNode {
    protected JvmReferenceTypeSignatureNode(
            NodeSchema<?> schema, TokenizableSource source, int start, int end) {
        super(schema, source, start, end);
    }
}
