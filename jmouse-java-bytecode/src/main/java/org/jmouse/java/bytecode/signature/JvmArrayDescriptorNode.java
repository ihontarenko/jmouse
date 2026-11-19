package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.descriptor.JavaArrayDescriptor;

public final class JvmArrayDescriptorNode extends JvmDescriptorTypeNode {
    public static final NodeKind<JvmArrayDescriptorNode> KIND =
            new NodeKind<>(JvmTextNames.ARRAY_DESCRIPTOR, JvmArrayDescriptorNode.class);
    public static final ChildRole<JvmDescriptorTypeNode> ELEMENT =
            ChildRole.required(JvmTextNames.ELEMENT, JvmDescriptorTypeNode.class);
    public static final NodeSchema<JvmArrayDescriptorNode> SCHEMA = NodeSchema.of(KIND, ELEMENT);

    private final int dimensions;

    public JvmArrayDescriptorNode(
            TokenizableSource source,
            int start,
            int end,
            int dimensions,
            JvmDescriptorTypeNode element) {
        super(SCHEMA, source, start, end);
        new JavaArrayDescriptor(element.descriptor(), dimensions);
        this.dimensions = dimensions;
        setChild(ELEMENT, element);
    }

    public int dimensions() {
        return dimensions;
    }

    public JvmDescriptorTypeNode element() {
        return child(ELEMENT);
    }

    @Override
    public JavaArrayDescriptor descriptor() {
        return new JavaArrayDescriptor(element().descriptor(), dimensions);
    }
}
