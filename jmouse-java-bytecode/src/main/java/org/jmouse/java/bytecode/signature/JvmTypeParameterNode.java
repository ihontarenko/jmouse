package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

import java.util.List;
import java.util.Optional;

public final class JvmTypeParameterNode extends JvmTextNode {
    public static final NodeKind<JvmTypeParameterNode> KIND =
            new NodeKind<>(JvmTextNames.TYPE_PARAMETER, JvmTypeParameterNode.class);
    public static final ChildRole<JvmReferenceTypeSignatureNode> CLASSBOUND =
            ChildRole.optional(JvmTextNames.CLASS_BOUND, JvmReferenceTypeSignatureNode.class);
    public static final ChildRole<JvmReferenceTypeSignatureNode> INTERFACEBOUNDS =
            ChildRole.list(JvmTextNames.INTERFACE_BOUNDS, JvmReferenceTypeSignatureNode.class);
    public static final NodeSchema<JvmTypeParameterNode> SCHEMA =
            NodeSchema.of(KIND, CLASSBOUND, INTERFACEBOUNDS);
    private final String name;

    public JvmTypeParameterNode(
            TokenizableSource source,
            int start,
            int end,
            String name,
            JvmReferenceTypeSignatureNode classBound,
            List<JvmReferenceTypeSignatureNode> interfaceBounds) {
        super(SCHEMA, source, start, end);
        this.name = name;
        setChild(CLASSBOUND, classBound);
        interfaceBounds.forEach(value -> addChild(INTERFACEBOUNDS, value));
    }

    public String name() {
        return name;
    }

    public Optional<JvmReferenceTypeSignatureNode> classBound() {
        return optionalChild(CLASSBOUND);
    }

    public List<JvmReferenceTypeSignatureNode> interfaceBounds() {
        return children(INTERFACEBOUNDS);
    }
}
