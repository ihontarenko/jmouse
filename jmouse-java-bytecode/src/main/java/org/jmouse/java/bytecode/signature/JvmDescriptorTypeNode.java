package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;

/** A syntactic JVM descriptor type with an explicit erased semantic value. */
public abstract class JvmDescriptorTypeNode extends JvmTypeSyntaxNode {
    protected JvmDescriptorTypeNode(
            NodeSchema<?> schema, TokenizableSource source, int start, int end) {
        super(schema, source, start, end);
    }

    public abstract JavaTypeDescriptor descriptor();
}
