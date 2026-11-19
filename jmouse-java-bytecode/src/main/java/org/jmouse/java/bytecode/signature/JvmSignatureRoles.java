package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRole;

public final class JvmSignatureRoles {
    public static final ParseRole<JvmTextInput, JvmTypeSyntaxNode> TYPE =
            new ParseRole<>(
                    JvmTextNames.JAVA_TYPE_SIGNATURE, JvmTextInput.class, JvmTypeSyntaxNode.class);
    public static final ParseRole<JvmTextInput, JvmReferenceTypeSignatureNode> REFERENCE =
            new ParseRole<>(
                    JvmTextNames.REFERENCE_TYPE_SIGNATURE,
                    JvmTextInput.class,
                    JvmReferenceTypeSignatureNode.class);
    public static final ParseRole<JvmTextInput, JvmClassTypeSignatureNode> CLASS_TYPE =
            new ParseRole<>(
                    JvmTextNames.CLASS_TYPE_SIGNATURE,
                    JvmTextInput.class,
                    JvmClassTypeSignatureNode.class);
    public static final ParseRole<JvmTextInput, JvmClassTypeSegmentNode> SEGMENT =
            new ParseRole<>(
                    JvmTextNames.CLASS_TYPE_SEGMENT,
                    JvmTextInput.class,
                    JvmClassTypeSegmentNode.class);
    public static final ParseRole<JvmTextInput, JvmTypeArgumentNode> ARGUMENT =
            new ParseRole<>(
                    JvmTextNames.TYPE_ARGUMENT, JvmTextInput.class, JvmTypeArgumentNode.class);
    public static final ParseRole<JvmTextInput, JvmTypeParameterNode> TYPE_PARAMETER =
            new ParseRole<>(
                    JvmTextNames.TYPE_PARAMETER, JvmTextInput.class, JvmTypeParameterNode.class);
    public static final ParseRole<JvmTextInput, JvmThrowsSignatureNode> THROWS =
            new ParseRole<>(
                    JvmTextNames.THROWS_SIGNATURE,
                    JvmTextInput.class,
                    JvmThrowsSignatureNode.class);
    public static final ParseRole<JvmTextInput, JvmClassSignatureNode> CLASS =
            new ParseRole<>(
                    JvmTextNames.CLASS_SIGNATURE, JvmTextInput.class, JvmClassSignatureNode.class);
    public static final ParseRole<JvmTextInput, JvmMethodSignatureNode> METHOD =
            new ParseRole<>(
                    JvmTextNames.METHOD_SIGNATURE,
                    JvmTextInput.class,
                    JvmMethodSignatureNode.class);

    private JvmSignatureRoles() {}
}
