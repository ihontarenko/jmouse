package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

import java.util.List;

public final class JvmMethodSignatureNode extends JvmTextNode {
    public static final NodeKind<JvmMethodSignatureNode> KIND =
            new NodeKind<>(JvmTextNames.METHOD_SIGNATURE, JvmMethodSignatureNode.class);
    public static final ChildRole<JvmTypeParameterNode> TYPE_PARAMETERS =
            ChildRole.list(JvmTextNames.TYPE_PARAMETERS, JvmTypeParameterNode.class);
    public static final ChildRole<JvmTypeSyntaxNode> PARAMETERS =
            ChildRole.list(JvmTextNames.PARAMETERS, JvmTypeSyntaxNode.class);
    public static final ChildRole<JvmTypeSyntaxNode> RESULT =
            ChildRole.required(JvmTextNames.RESULT, JvmTypeSyntaxNode.class);
    public static final ChildRole<JvmThrowsSignatureNode> THROWN_TYPES =
            ChildRole.list(JvmTextNames.THROWN_TYPES, JvmThrowsSignatureNode.class);
    public static final NodeSchema<JvmMethodSignatureNode> SCHEMA =
            NodeSchema.of(KIND, TYPE_PARAMETERS, PARAMETERS, RESULT, THROWN_TYPES);

    public JvmMethodSignatureNode(
            TokenizableSource source,
            int start,
            int end,
            List<JvmTypeParameterNode> typeParameters,
            List<JvmTypeSyntaxNode> parameters,
            JvmTypeSyntaxNode result,
            List<JvmThrowsSignatureNode> thrownTypes) {
        super(SCHEMA, source, start, end);

        typeParameters.forEach(value -> addChild(TYPE_PARAMETERS, value));
        parameters.forEach(value -> addChild(PARAMETERS, value));
        setChild(RESULT, result);
        thrownTypes.forEach(value -> addChild(THROWN_TYPES, value));
    }

    public List<JvmTypeParameterNode> typeParameters() {
        return children(TYPE_PARAMETERS);
    }

    public List<JvmTypeSyntaxNode> parameters() {
        return children(PARAMETERS);
    }

    public JvmTypeSyntaxNode result() {
        return child(RESULT);
    }

    public List<JvmThrowsSignatureNode> thrownTypes() {
        return children(THROWN_TYPES);
    }
}
