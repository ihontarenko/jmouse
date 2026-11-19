package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;

import java.util.List;

public final class JvmMethodDescriptorNode extends JvmTextNode {
    public static final NodeKind<JvmMethodDescriptorNode> KIND =
            new NodeKind<>(JvmTextNames.METHOD_DESCRIPTOR, JvmMethodDescriptorNode.class);
    public static final ChildRole<JvmDescriptorTypeNode> PARAMETERS =
            ChildRole.list(JvmTextNames.PARAMETERS, JvmDescriptorTypeNode.class);
    public static final ChildRole<JvmDescriptorTypeNode> RESULT =
            ChildRole.required(JvmTextNames.RESULT, JvmDescriptorTypeNode.class);
    public static final NodeSchema<JvmMethodDescriptorNode> SCHEMA =
            NodeSchema.of(KIND, PARAMETERS, RESULT);

    public JvmMethodDescriptorNode(
            TokenizableSource source,
            int start,
            int end,
            List<JvmDescriptorTypeNode> parameters,
            JvmDescriptorTypeNode result) {
        super(SCHEMA, source, start, end);
        parameters.forEach(parameter -> addChild(PARAMETERS, parameter));
        setChild(RESULT, result);
    }

    public List<JvmDescriptorTypeNode> parameters() {
        return children(PARAMETERS);
    }

    public JvmDescriptorTypeNode result() {
        return child(RESULT);
    }

    public JavaMethodDescriptor descriptor() {
        return new JavaMethodDescriptor(
                parameters().stream().map(JvmDescriptorTypeNode::descriptor).toList(),
                result().descriptor());
    }
}
