package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;

public final class JvmClassDescriptorNode extends JvmDescriptorTypeNode {
    public static final NodeKind<JvmClassDescriptorNode> KIND =
            new NodeKind<>(JvmTextNames.CLASS_DESCRIPTOR, JvmClassDescriptorNode.class);
    public static final NodeSchema<JvmClassDescriptorNode> SCHEMA = NodeSchema.of(KIND);

    private final JavaClassDescriptor descriptor;

    public JvmClassDescriptorNode(
            TokenizableSource source, int start, int end, String internalName) {
        super(SCHEMA, source, start, end);
        descriptor = new JavaClassDescriptor(internalName);
    }

    @Override
    public JavaClassDescriptor descriptor() {
        return descriptor;
    }
}
