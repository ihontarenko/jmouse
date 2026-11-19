package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

public final class JvmArraySignatureNode extends JvmReferenceTypeSignatureNode {
    public static final NodeKind<JvmArraySignatureNode> KIND =
            new NodeKind<>(JvmTextNames.ARRAY_SIGNATURE, JvmArraySignatureNode.class);
    public static final ChildRole<JvmTypeSyntaxNode> ELEMENT =
            ChildRole.required(JvmTextNames.ELEMENT, JvmTypeSyntaxNode.class);
    public static final NodeSchema<JvmArraySignatureNode> SCHEMA = NodeSchema.of(KIND, ELEMENT);
    private final int dimensions;

    public JvmArraySignatureNode(
            TokenizableSource source,
            int start,
            int end,
            int dimensions,
            JvmTypeSyntaxNode element) {
        super(SCHEMA, source, start, end);
        this.dimensions = dimensions;
        setChild(ELEMENT, element);
    }

    public int dimensions() {
        return dimensions;
    }

    public JvmTypeSyntaxNode element() {
        return child(ELEMENT);
    }
}
