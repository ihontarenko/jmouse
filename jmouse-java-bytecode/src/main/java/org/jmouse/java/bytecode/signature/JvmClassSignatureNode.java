package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

import java.util.List;

public final class JvmClassSignatureNode extends JvmTextNode {
    public static final NodeKind<JvmClassSignatureNode> KIND =
            new NodeKind<>(JvmTextNames.CLASS_SIGNATURE, JvmClassSignatureNode.class);
    public static final ChildRole<JvmTypeParameterNode> TYPE_PARAMETERS =
            ChildRole.list(JvmTextNames.TYPE_PARAMETERS, JvmTypeParameterNode.class);
    public static final ChildRole<JvmClassTypeSignatureNode> SUPERCLASS =
            ChildRole.required(JvmTextNames.SUPERCLASS, JvmClassTypeSignatureNode.class);
    public static final ChildRole<JvmClassTypeSignatureNode> INTERFACES =
            ChildRole.list(JvmTextNames.INTERFACES, JvmClassTypeSignatureNode.class);
    public static final NodeSchema<JvmClassSignatureNode> SCHEMA =
            NodeSchema.of(KIND, TYPE_PARAMETERS, SUPERCLASS, INTERFACES);

    public JvmClassSignatureNode(
            TokenizableSource source,
            int start,
            int end,
            List<JvmTypeParameterNode> typeParameters,
            JvmClassTypeSignatureNode superclass,
            List<JvmClassTypeSignatureNode> interfaces) {
        super(SCHEMA, source, start, end);

        typeParameters.forEach(value -> addChild(TYPE_PARAMETERS, value));
        setChild(SUPERCLASS, superclass);
        interfaces.forEach(value -> addChild(INTERFACES, value));
    }

    public List<JvmTypeParameterNode> typeParameters() {
        return children(TYPE_PARAMETERS);
    }

    public JvmClassTypeSignatureNode superclass() {
        return child(SUPERCLASS);
    }

    public List<JvmClassTypeSignatureNode> interfaces() {
        return children(INTERFACES);
    }
}
