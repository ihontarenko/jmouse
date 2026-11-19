package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.NodeSchema;

/** Common syntax contract for erased descriptors and generic type signatures. */
public abstract class JvmTypeSyntaxNode extends JvmTextNode {
    protected JvmTypeSyntaxNode(
            NodeSchema<?> schema, TokenizableSource source, int start, int end) {
        super(schema, source, start, end);
    }
}
