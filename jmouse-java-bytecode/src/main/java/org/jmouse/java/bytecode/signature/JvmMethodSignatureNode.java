package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

import java.util.List;

public final class JvmMethodSignatureNode extends JvmTextNode {
    public static final NodeKind<JvmMethodSignatureNode> KIND =
            new NodeKind<>(JvmTextNames.METHOD_SIGNATURE, JvmMethodSignatureNode.class);
    public static final ChildRole<JvmTypeParameterNode> TYPEPARAMETERS =
            ChildRole.list(JvmTextNames.TYPE_PARAMETERS, JvmTypeParameterNode.class);
    public static final ChildRole<JvmTypeSyntaxNode> PARAMETERS =
            ChildRole.list(JvmTextNames.PARAMETERS, JvmTypeSyntaxNode.class);
    public static final ChildRole<JvmTypeSyntaxNode> RESULT =
            ChildRole.required(JvmTextNames.RESULT, JvmTypeSyntaxNode.class);
    public static final ChildRole<JvmThrowsSignatureNode> THROWNTYPES =
            ChildRole.list(JvmTextNames.THROWN_TYPES, JvmThrowsSignatureNode.class);
    public static final NodeSchema<JvmMethodSignatureNode> SCHEMA =
            NodeSchema.of(KIND, TYPEPARAMETERS, PARAMETERS, RESULT, THROWNTYPES);

    public JvmMethodSignatureNode(
            TokenizableSource source,
            int start,
            int end,
            List<JvmTypeParameterNode> typeParameters,
            List<JvmTypeSyntaxNode> parameters,
            JvmTypeSyntaxNode result,
            List<JvmThrowsSignatureNode> thrownTypes) {
        super(SCHEMA, source, start, end);

        typeParameters.forEach(value -> addChild(TYPEPARAMETERS, value));
        parameters.forEach(value -> addChild(PARAMETERS, value));
        setChild(RESULT, result);
        thrownTypes.forEach(value -> addChild(THROWNTYPES, value));
    }

    public List<JvmTypeParameterNode> typeParameters() {
        return children(TYPEPARAMETERS);
    }

    public List<JvmTypeSyntaxNode> parameters() {
        return children(PARAMETERS);
    }

    public JvmTypeSyntaxNode result() {
        return child(RESULT);
    }

    public List<JvmThrowsSignatureNode> thrownTypes() {
        return children(THROWNTYPES);
    }
}
