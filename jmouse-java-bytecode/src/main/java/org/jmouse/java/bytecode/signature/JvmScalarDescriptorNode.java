package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.java.model.type.JavaVoidType;

public final class JvmScalarDescriptorNode extends JvmDescriptorTypeNode {
    public static final NodeKind<JvmScalarDescriptorNode> KIND =
            new NodeKind<>(JvmTextNames.SCALAR_DESCRIPTOR, JvmScalarDescriptorNode.class);
    public static final NodeSchema<JvmScalarDescriptorNode> SCHEMA = NodeSchema.of(KIND);

    private final JavaTypeDescriptor descriptor;

    public JvmScalarDescriptorNode(
            TokenizableSource source, int start, int end, JavaTypeDescriptor descriptor) {
        super(SCHEMA, source, start, end);
        if (!(descriptor instanceof JavaPrimitiveType) && descriptor != JavaVoidType.VOID) {
            throw new IllegalArgumentException(
                    "A scalar descriptor requires a primitive or void type");
        }
        this.descriptor = descriptor;
    }

    @Override
    public JavaTypeDescriptor descriptor() {
        return descriptor;
    }
}
